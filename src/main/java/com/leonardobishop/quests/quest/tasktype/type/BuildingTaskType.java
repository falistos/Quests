package com.leonardobishop.quests.quest.tasktype.type;

import com.leonardobishop.quests.QuestsConfigLoader;
import com.leonardobishop.quests.api.QuestsAPI;
import com.leonardobishop.quests.player.QPlayer;
import com.leonardobishop.quests.player.questprogressfile.QuestProgress;
import com.leonardobishop.quests.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.quest.Quest;
import com.leonardobishop.quests.quest.Task;
import com.leonardobishop.quests.quest.tasktype.ConfigValue;
import com.leonardobishop.quests.quest.tasktype.TaskType;
import com.leonardobishop.quests.quest.tasktype.TaskUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class BuildingTaskType extends TaskType {

    private List<ConfigValue> creatorConfigValues = new ArrayList<>();

    public BuildingTaskType() {
        super("blockplace", TaskUtils.TASK_ATTRIBUTION_STRING, "Place a set amount of blocks.");
        this.creatorConfigValues.add(new ConfigValue("amount", true, "Amount of blocks to be placed."));
        this.creatorConfigValues.add(new ConfigValue("worlds", false, "Permitted worlds the player must be in."));
    }

    @Override
    public List<QuestsConfigLoader.ConfigProblem> detectProblemsInConfig(String root, HashMap<String, Object> config) {
        ArrayList<QuestsConfigLoader.ConfigProblem> problems = new ArrayList<>();
        if (TaskUtils.configValidateExists(root + ".amount", config.get("amount"), problems, "amount", super.getType()))
            TaskUtils.configValidateInt(root + ".amount", config.get("amount"), problems, false, true, "amount");
        return problems;
    }

    @Override
    public List<ConfigValue> getCreatorConfigValues() {
        return creatorConfigValues;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) return;

        QPlayer qPlayer = QuestsAPI.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
        if (qPlayer == null) {
            return;
        }

        for (Quest quest : super.getRegisteredQuests()) {
            if (qPlayer.hasStartedQuest(quest)) {
                QuestProgress questProgress = qPlayer.getQuestProgressFile().getQuestProgress(quest);

                for (Task task : quest.getTasksOfType(super.getType())) {
                    if (!TaskUtils.validateWorld(event.getPlayer(), task)) continue;

                    TaskProgress taskProgress = questProgress.getTaskProgress(task.getId());

                    if (taskProgress.isCompleted()) {
                        continue;
                    }

                    int brokenBlocksNeeded = (int) task.getConfigValue("amount");

                    int progressBlocksBroken;
                    if (taskProgress.getProgress() == null) {
                        progressBlocksBroken = 0;
                    } else {
                        progressBlocksBroken = (int) taskProgress.getProgress();
                    }

                    taskProgress.setProgress(progressBlocksBroken + 1);

                    if (((int) taskProgress.getProgress()) >= brokenBlocksNeeded) {
                        taskProgress.setCompleted(true);
                    }
                }
            }
        }
    }

}