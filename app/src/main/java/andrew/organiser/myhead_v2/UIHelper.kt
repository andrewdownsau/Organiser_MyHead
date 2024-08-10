package andrew.organiser.myhead_v2

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import java.time.Duration
import java.time.LocalDate


class UIHelper {
    companion object {

        private fun createGenericButton(c: Context, btnText : String) : Button{
            val dynamicButton = Button(c)

            // setting layout_width and layout_height using layout parameters
            val params = ConstraintLayout.LayoutParams(1000, 200)
            params.setMargins(15, 15, 15, 15)
            dynamicButton.setLayoutParams(params)
            dynamicButton.isAllCaps = false
            dynamicButton.textSize = 20.0F
            dynamicButton.setBackgroundResource(R.drawable.shadow_button)
            dynamicButton.id = View.generateViewId()

            //Set text new line based on number of characters (max 30 characters per line)
            var displayedText = btnText
            if(btnText.length > 32){
                displayedText = btnText.take(32)
                displayedText = displayedText.take(displayedText.lastIndexOf(" "))
                displayedText += "\n" + btnText.drop(displayedText.length)
            }
            dynamicButton.text = displayedText

            return dynamicButton
        }

        fun createContextButtonList(c: Context, layout: ConstraintLayout, fileContents: List<String>, navCtrl: NavController){
            println("+++ createContextButtonList UIHelper +++")
            var lastBtnId = 0

            // Clear all views from layout and add all buttons from contents
            layout.removeAllViews()

            //Sort the list based on each top task (if present)
            var topTasksStr = ""
            val taskContents = FileHelper.readFile(c, MainActivity.TASK_FILE)
            if(taskContents != null){
                val sortedTasks = FileHelper.sortTaskList(taskContents)
                fileContents.forEach {
                    //Extract context name and then add topmost task for context
                    val contextName = FileHelper.getObjectValue(it, "Name:")
                    if(contextName != null){
                        val sortedTasksInContext = sortedTasks.filter { task -> task.contains("Context:$contextName\t") }
                        topTasksStr += "Name:$contextName\t"
                        if(sortedTasksInContext.isNotEmpty()) topTasksStr += sortedTasksInContext[0]
                        topTasksStr += "\n"
                    }
                }
                val topTaskList = topTasksStr.split("\n")
                val sortedTopTaskList = FileHelper.sortTaskList(topTaskList)
                println("_topTasks: $sortedTopTaskList")

                //Create context button list
                sortedTopTaskList.forEach {
                    if (it.isNotEmpty()) {
                        lastBtnId = createContextButton(c, layout, lastBtnId, it, navCtrl)
                    }
                }
            }
            else{
                //Create context button list without any tasks attached
                fileContents.forEach {
                    if (it.isNotEmpty()) {
                        lastBtnId = createContextButton(c, layout, lastBtnId, it, navCtrl)
                    }
                }
            }
        }

        private fun createContextButton(c: Context, layout: ConstraintLayout, topId: Int, contextObject: String, navCtrl: NavController) : Int {
            val contextName = FileHelper.getObjectValue(contextObject, "Name:")
            if(contextName != null){
                println("+++ createContextButton: $contextName +++")
                val dynamicButton = createGenericButton(c, contextName)
                val contextBundle = bundleOf("contextObject" to contextObject)
                setButtonBackground(contextObject, dynamicButton)
                setButtonToLayout(layout, dynamicButton, topId)

                //Set on long click listener to trigger edit page function for non-tasks
                dynamicButton.setOnLongClickListener {
                    navCtrl.navigate(R.id.action_ContextList_to_ContextAddEdit, contextBundle)
                    true
                }
                //Set onclick listener to send to task list fragment
                dynamicButton.setOnClickListener {
                    navCtrl.navigate(R.id.action_ContextList_to_TaskList, contextBundle)
                }

                //Return button id for next button position
                return dynamicButton.id
            }
            return 0
        }

        fun createTaskButtonList(c: Context, layout: ConstraintLayout, taskList: List<String>, navCtrl: NavController){
            println("+++ createTaskButtonList: $taskList +++")
            var lastBtnId = 0

            //Check whether any tasks have been un-completed in the archive list
            var outputTaskList = taskList
            if(navCtrl.currentDestination?.id == R.id.TaskArchive){
                if(taskList.joinToString("\n").contains("Completed:${MainActivity.DEFAULT_DATE}\t")){
                    outputTaskList = FileHelper.unArchiveTasks(c, taskList)
                }
            }

            // Clear all views from layout and add all buttons from contents
            layout.removeAllViews()
            outputTaskList.forEach {
                if (it.isNotEmpty()) {
                    lastBtnId = createTaskButton(c, layout, lastBtnId, outputTaskList, it, navCtrl)
                }
            }
        }

        private fun createTaskButton(c: Context, layout: ConstraintLayout, topId: Int, taskList: List<String>, taskObject: String,  navCtrl: NavController) : Int {
            println("+++ createTaskButton: \n$taskObject +++")

            //Extract ID and name from the content line
            val taskName = FileHelper.getObjectValue(taskObject, "TaskName:")
            if(taskName != null){
                val dynamicButton = createGenericButton(c, taskName)
                setButtonBackground(taskObject, dynamicButton)
                setButtonToLayout(layout, dynamicButton, topId)


                //Set on long click listener to trigger complete status for task
                dynamicButton.setOnLongClickListener {
                    //Set completed flag to true for task and Re-create button list
                    val fileName = when(navCtrl.currentDestination?.id){
                        R.id.TaskArchive -> MainActivity.ARCHIVE_FILE
                        else -> MainActivity.TASK_FILE
                    }
                    val newContents = FileHelper.changeTaskStatus(c, taskObject, taskList, fileName)
                    createTaskButtonList(c, layout, FileHelper.sortTaskList(newContents), navCtrl)
                    true
                }
                //Set onclick listener to send to task edit page
                dynamicButton.setOnClickListener {
                    //Do not allow if in archive
                    if(navCtrl.currentDestination?.id != R.id.TaskArchive){
                        val contextName = FileHelper.getObjectValue(taskObject, "Context:")
                        if(contextName != null){
                            val contextObject = "Name:${contextName}\t"
                            val taskObjectBundle = bundleOf("taskObject" to taskObject, "contextObject" to contextObject)

                            //Navigate depending on current fragment
                            val navActionId = when(navCtrl.currentDestination?.id){
                                R.id.TaskList -> R.id.action_TaskList_to_TaskAddEdit
                                R.id.UrgentList -> R.id.action_UrgentList_to_TaskAddEdit
                                //R.id.TaskArchive -> R.id.action_TaskArchive_to_TaskAddEdit
                                else -> R.id.action_TaskList_to_TaskAddEdit
                            }
                            navCtrl.navigate(navActionId, taskObjectBundle)
                        }
                    }
                }
                return dynamicButton.id
            }
            return 0
        }

        private fun setButtonBackground(itemObject: String, dynamicButton: Button){
            var btnBackgroundResource = R.drawable.shadow_button_white
            println("_Setting background of task:\n$itemObject")

            // --- Check 1: Is it a valid task
            if(itemObject.contains("TaskName:")){
                try {
                    // --- Check 2: Is the task completed? ---
                    val completedStatus = FileHelper.getObjectValue(itemObject, "Completed:")
                    if(completedStatus != null && completedStatus != MainActivity.DEFAULT_DATE){
                        dynamicButton.paintFlags += Paint.STRIKE_THRU_TEXT_FLAG
                        btnBackgroundResource = R.drawable.shadow_button_grey
                    }
                    else{
                        //Ensure that formatting is reversed if needed to un-complete task
                        if ((dynamicButton.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG) > 0)
                            dynamicButton.paintFlags = dynamicButton.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

                        // --- Check 3: Is the task conditional of others or time ---
                        val startDateStr = FileHelper.getObjectValue(itemObject, "StartDate:")
                        val taskCondition = FileHelper.getObjectValue(itemObject, "Condition:") != "None"
                        if(startDateStr != null) {
                            val todayDate = MainActivity.SIMPLE_DF.parse(LocalDate.now().format(MainActivity.DATE_FORMAT))
                            val startDate = MainActivity.SIMPLE_DF.parse(startDateStr)
                            if ((startDate != null && startDate > todayDate) || taskCondition) {
                                btnBackgroundResource = R.drawable.shadow_button_blue
                            }
                            else {
                                // --- Check 4: Is the task complexity less than 3 ---
                                val complexity = FileHelper.getObjectValue(itemObject, "Complexity:")?.toInt()
                                if(complexity != null && complexity < 3)
                                    dynamicButton.paintFlags += Paint.UNDERLINE_TEXT_FLAG

                                // --- Check 5: Is the task urgent, if so, how urgent? ---
                                val dueDateStr = FileHelper.getObjectValue(itemObject, "DueDate:")
                                if(dueDateStr != null) {
                                    val dueDate = MainActivity.SIMPLE_DF.parse(dueDateStr)?.toInstant()

                                    if(dueDate != null && todayDate != null) {
                                        btnBackgroundResource = when (Duration.between(todayDate.toInstant(), dueDate).toDays()){
                                            // --- Check 5.1: Is the task due past 7 days ---
                                            in 8 .. Int.MAX_VALUE -> R.drawable.shadow_button_green
                                            // --- Check 5.2: Is the task due in 3 to 7 days ---
                                            in 4 .. 7 -> R.drawable.shadow_button_yellow
                                            // --- Check 5.3: Is the task due in 1 to 3 days ---
                                            in 1 .. 3 -> R.drawable.shadow_button_orange
                                            // --- Check 5.4: Is the task due today ---
                                            0L -> R.drawable.shadow_button_red
                                            // --- Check 5.4: Is the task overdue ---
                                            else -> R.drawable.shadow_button_dark_red
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (e :Exception){
                    println("___ Error: $e ___")
                }
            }
            dynamicButton.setBackgroundResource(btnBackgroundResource)
        }

        private fun setButtonToLayout(layout: ConstraintLayout, dynamicButton: Button, topId: Int){
            println("Setting ${dynamicButton.text} to layout - topId: $topId")
            // add Button to Layout
            layout.addView(dynamicButton)
            val constraintSet = ConstraintSet()
            constraintSet.clone(layout)
            constraintSet.connect(dynamicButton.id, ConstraintSet.START, layout.id, ConstraintSet.START, 0)
            constraintSet.connect(dynamicButton.id, ConstraintSet.END, layout.id, ConstraintSet.END, 0)
            if(topId == 0)
                constraintSet.connect(dynamicButton.id, ConstraintSet.TOP, topId, ConstraintSet.TOP, 0)
            else
                constraintSet.connect(dynamicButton.id, ConstraintSet.TOP, topId, ConstraintSet.BOTTOM, 30)

            constraintSet.applyTo(layout)
        }

    }
}