package andrew.organiser.myhead

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


class UIHelper {
    companion object {

        private const val STANDARD_TASK_OBJECT_SIZE = 14

        fun createContextButtonList(c: Context, layout: ConstraintLayout, fileContents: String, navCtrl: NavController){
            println("+++ createContextButtonList UIHelper +++")
            var lastBtnId = 0

            // Clear all views from layout and add all buttons from contents
            layout.removeAllViews()

            //Sort the list based on each top task (if present)
            var topTasks = ""
            fileContents.split("\n").drop(1).forEach {
                val contextFileName = it.replace(";", "_") + ".txt"
                topTasks += "\n${contextFileName}:${getTopTask(c, contextFileName)}"
            }
            println("_topTasks: $topTasks")
            val orderedContents = FileHelper.sortTaskList(topTasks)
            println("_orderedContents: $orderedContents")


            //Create context button in order of top task
            orderedContents.split("\n").forEach {
                if (it.isNotEmpty()) {
                    val contextFileName = it.take(it.indexOf(".txt"))
                    lastBtnId = createContextButton(c, layout, lastBtnId, contextFileName, navCtrl)
                }
            }
        }

        fun createTaskButtonList(c: Context, layout: ConstraintLayout, fileName :String, contextContents: String, navCtrl: NavController){
            println("+++ createTaskButtonList UIHelper +++")
            var lastBtnId = 0

            // Clear all views from layout and add all buttons from contents
            layout.removeAllViews()
            contextContents.split("\n").forEach {
                if (it.isNotEmpty()) {
                    lastBtnId = createTaskButton(c, layout, lastBtnId, fileName, it, navCtrl)
                }
            }
        }

        private fun createContextButton(c: Context, layout: ConstraintLayout, topId: Int, contextFileName: String, navCtrl: NavController) : Int {
            println("+++ createContextButton UIHelper +++")
            var itemId = 0
            if(contextFileName.contains("_") && contextFileName.split("_").size == 2){
                try{
                    itemId = contextFileName.split("_")[0].toInt()
                    val btnText = contextFileName.split("_")[1]
                    val dynamicButton = createGenericButton(c, btnText, itemId)
                    val actualFile = "$contextFileName.txt"
                    setButtonBackground(getTopTask(c, actualFile), dynamicButton)
                    setButtonToLayout(layout, dynamicButton, topId)

                    //Set on long click listener to trigger edit page function for non-tasks
                    dynamicButton.setOnLongClickListener {
                        navCtrl.navigate(R.id.action_ContextList_to_ContextAddEdit, bundleOf("itemId" to itemId))
                        true
                    }
                    //Set onclick listener to send to task list fragment
                    dynamicButton.setOnClickListener {
                        navCtrl.navigate(R.id.action_ContextList_to_TaskList, bundleOf("contextFileName" to contextFileName))
                    }
                }catch (e: Exception){
                    println("___ Error: $e ___")
                }

            }

            //Return button id for next button position
            return itemId
        }

        private fun createTaskButton(c: Context, layout: ConstraintLayout, topId: Int, fileName :String, contentLine: String,  navCtrl: NavController) : Int {
            println("+++ createTaskButton UIHelper +++")

            //Extract ID and name from the content line
            val btnId = contentLine.split(";")[0].toInt()
            val btnName = contentLine.split(";")[1]
            val dynBtn = createGenericButton(c, btnName, btnId)
            setButtonBackground(contentLine, dynBtn)
            setButtonToLayout(layout, dynBtn, topId)


            //Set on long click listener to trigger complete status for task
            dynBtn.setOnLongClickListener {
                //Set completed flag to true for task and Re-create button list
                val newContents = FileHelper.changeTaskStatus(c, fileName, contentLine)
                createTaskButtonList(c, layout, fileName, FileHelper.sortTaskList(newContents), navCtrl)
                true
            }
            //Set onclick listener to send to task edit page
            dynBtn.setOnClickListener {
                val taskAddEditBundle = bundleOf("fileName" to fileName, "contentLine" to contentLine)
                navCtrl.navigate(R.id.action_TaskList_to_TaskAddEdit, taskAddEditBundle)
            }

            return btnId
        }

        private fun createGenericButton(c: Context, btnText : String, btnId : Int) : Button{
            println("_Creating Button: $btnText")
            val dynamicButton = Button(c)

            // setting layout_width and layout_height using layout parameters
            val params = ConstraintLayout.LayoutParams(1000, 200)
            params.setMargins(15, 15, 15, 15)
            dynamicButton.setLayoutParams(params)
            dynamicButton.isAllCaps = false

            // Setting button id value
            dynamicButton.setId(btnId)

            dynamicButton.text = btnText
            dynamicButton.textSize = 20.0F
            dynamicButton.setBackgroundResource(R.drawable.shadow_button)

            return dynamicButton
        }

        private fun setButtonBackground(contentLine: String, dynamicButton: Button){
            var btnBackgroundResource = R.drawable.shadow_button_white
            println("_Setting background of task:\n$contentLine")
            val taskObject = contentLine.split(";")
            try{
                val completeStatus = taskObject.last()
                if(taskObject.size > 10 && completeStatus != "01/01/2000") {
                    dynamicButton.paintFlags += Paint.STRIKE_THRU_TEXT_FLAG
                    btnBackgroundResource = R.drawable.shadow_button_grey
                }
                else{
                    //Check the condition of start date and other tasks for blue status
                    val startDateStr = taskObject[7]
                    val taskCondition = taskObject[11] != "None"
                    val todayDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                    val startDate: Date? = sdf.parse(startDateStr)
                    val todayDate: Date? = sdf.parse(todayDateStr)

                    if ((startDate != null && startDate > todayDate) || taskCondition) {
                        btnBackgroundResource = R.drawable.shadow_button_blue
                    }
                    else{
                        //Check the complexity level of the task and bold complex lower than 3
                        if(taskObject[3].toInt() < 3)
                            dynamicButton.paintFlags += Paint.UNDERLINE_TEXT_FLAG

                        //Check the condition of due date and number of days from today
                        val dueDateStr = taskObject[8]
                        val dueDate: Date? = sdf.parse(dueDateStr)

                        if (dueDate != null) {
                            btnBackgroundResource = if(dueDate < todayDate) R.drawable.shadow_button_dark_red
                            else if(dueDate == todayDate) R.drawable.shadow_button_red
                            else{
                                val compareDate: Calendar = Calendar.getInstance()
                                compareDate.setTime(dueDate)
                                compareDate.add(Calendar.DATE, -4)
                                if(compareDate.time < todayDate) R.drawable.shadow_button_orange
                                else R.drawable.shadow_button_green

                            }
                        }
                    }

                    if ((dynamicButton.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG) > 0)
                        dynamicButton.paintFlags = dynamicButton.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

            }
            catch (e :Exception){
                println("___ Error: $e ___")
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

        private fun getTopTask(c: Context, contextFileName: String) : String{
            //Get sorted file read of context file
            var readOutput = FileHelper.checkFile(c, contextFileName, "", STANDARD_TASK_OBJECT_SIZE)
            readOutput = FileHelper.archiveTasks(c, contextFileName, readOutput)
            if (readOutput.isNotEmpty()){
                return FileHelper.sortTaskList(readOutput).split("\n")[0]
            }
            return ""
        }
    }
}