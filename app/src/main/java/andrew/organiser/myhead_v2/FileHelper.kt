package andrew.organiser.myhead_v2

import android.app.appsearch.Migrator
import android.content.Context
import java.io.File
import java.io.IOException
import java.time.LocalDate

class FileHelper {
    companion object {

        private fun overwriteFile(c: Context, fileName: String, content: String){
            println("--- overwriteFileContents: $content ---")

            //Make sure there is at least one newline character at the end of the string\
            var writeContent = content
            if(!writeContent.contains("\n")) writeContent += "\n"

            c.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(writeContent.toByteArray())
            }
        }

        private fun extractTaskList(rawTaskList: List<String>?): MutableList<String>?{
            if(rawTaskList != null){
                var taskNameListStr = ""
                rawTaskList.forEach {
                    taskNameListStr += getObjectValue(it, "TaskName:") + "\n"
                }
                return taskNameListStr.split("\n").toMutableList()
            }
            else return null
        }

        private fun addTimeToDate(frequencyType:String, originalDateStr: String): LocalDate{
            val dayMovement = when(frequencyType){
                "Daily" -> 1
                "Weekly" -> 7
                "Fortnightly" -> 14
                else -> 0
            }
            val monthMovement = when(frequencyType){
                "Monthly" -> 1
                "Quarterly" -> 3
                "Yearly" -> 12
                else -> 0
            }

            //Change start and due date by the required time jumps
            var date = LocalDate.parse(originalDateStr, MainActivity.DATE_FORMAT)

            date = date.plusDays(dayMovement.toLong())
            date = date.plusMonths(monthMovement.toLong())

            return date
        }

        private fun deleteTasksFromFiles(c: Context, contextObject: String){
            //Delete all tasks in both main files
            val contextName = getObjectValue(contextObject, "Name:")
            if(contextName != null) {
                println("=== deleteTasksFromFiles: $contextName")
                try{
                    val taskRemainList = readFile(c, MainActivity.TASK_FILE)!!.filter { !it.contains("Context:$contextName\t") }
                    val archiveRemainList = readFile(c, MainActivity.ARCHIVE_FILE)!!.filter { !it.contains("Context:$contextName\t") }
                    //val taskRemainList = readFile(c, MainActivity.TASK_FILE)!!.filter { !it.contains(contextName) }
                    //val archiveRemainList = readFile(c, MainActivity.ARCHIVE_FILE)!!.filter { !it.contains(contextName) }

                    overwriteFile(c, MainActivity.TASK_FILE, taskRemainList.joinToString("\n"))
                    overwriteFile(c, MainActivity.ARCHIVE_FILE, archiveRemainList.joinToString("\n"))

                }catch (e: java.lang.Exception){
                    println("___ Error: $e ___")
                }
            }

        }

        fun readFile(c: Context, fileName: String) : MutableList<String>? {
            println("+++ FileHelper readFile: $fileName +++")
            var content = ""
            try {
                val file = File(c.filesDir, fileName)
                if (file.exists())
                    content = file.readText()
                else
                    println("___ Error: File does not exist ___")

            } catch (e: IOException) {
                //Log the exception
                println("___ Error: $e ___")
            }
            println("___ readFile content:$content")
            return if(content.contains("\n"))
                content.split("\n").toMutableList()
            else
                null
        }

        fun validateEntry(c: Context, oldName: String , newName: String, fileName: String): String{
            //If the values are already equal then it is the same entry, anything is valid
            if(oldName == newName) return "valid"
            else{
                //Check that the string is limited to 60 characters
                if(newName.length > 60)
                    return c.getString(R.string.max_character_warning)

                //Check for exact matches for newName in file
                val readFileOutput = readFile(c, fileName)
                if (!readFileOutput.isNullOrEmpty()) {
                    val readFileStr = readFileOutput.joinToString("\n")
                    if(readFileStr.contains(newName)){
                        return c.getString(R.string.clone_warning)
                    }
                }
                return "valid"
            }
        }

        fun appendFile(c: Context, fileName: String, content: String){
            println("--- appendFile: $fileName Contents: $content")

            //Make sure there is at least one newline character at the end of the string\
            var writeContent = content
            if(!writeContent.contains("\n")) writeContent += "\n"

            c.openFileOutput(fileName, Context.MODE_APPEND).use {
                it.write(writeContent.toByteArray())
            }
        }

        fun deleteEntry(c: Context, fileName: String, contentItem: String){
            //Destroy everything in file when corrupt for debugging
            if(contentItem == "All") {
                overwriteFile(c, fileName, "")
            }
            else{
                //Read entire file and remove line with matching Id
                val fileContents = readFile(c, fileName)
                var formattedItem = contentItem

                //If context, format object and delete all tasks attached to context in task and archive list
                if(fileName == MainActivity.CONTEXT_FILE){
                    formattedItem = "Name:${getObjectValue(formattedItem, "Name:")}\t"
                }
                println("+++ deleteEntry:$formattedItem from $fileName +++")
                if(fileContents != null && fileContents.remove(formattedItem)){
                    overwriteFile(c, fileName, fileContents.joinToString ("\n"))

                    if(fileName == MainActivity.CONTEXT_FILE){
                        deleteTasksFromFiles(c, formattedItem)
                    }
                }
            }
        }

        fun editEntry(c: Context, fileName: String, oldObject : String, newObject: String){
            //Debugging Logs
            println("+++ editEntry File: $fileName +++")

            //Reformat old context to not include task values if applicable
            var formattedOldObject = oldObject
            if(oldObject.contains("TaskName") && !newObject.contains("TaskName")){
                formattedOldObject = oldObject.split("\t")[0] + "\t"
            }
            println("oldObject:$formattedOldObject")
            println("newObject:$newObject")

            val fileContents = readFile(c, fileName)
            if(fileContents != null){
                val fileContentsStr = fileContents.joinToString("\n")
                overwriteFile(c, fileName, fileContentsStr.replace(formattedOldObject, newObject))
            }
        }

        fun getConditionList(c: Context, contextName: String, taskObject: String?): MutableList<String>{
            var outputTaskList = "None\n"
            val taskList = readFile(c, MainActivity.TASK_FILE)
            if(taskList != null){
                //Filter tasks that are completed or exist already
                taskList.remove(taskObject)
                val pendingTasks = taskList.filter { it.contains("Completed:01/01/2000") && it.contains(contextName)}
                val pendingTaskNames = extractTaskList(pendingTasks)
                if(pendingTaskNames != null){
                    outputTaskList += pendingTaskNames.joinToString("\n")
                }
            }

            println("_Get condition list output: \n$outputTaskList")
            return outputTaskList.split("\n").toMutableList()
        }

        fun changeTaskStatus(c: Context, taskObject: String, taskList: List<String>, fileName: String) : List<String>{
            //Change status of item based on current value
            println("--- changeTaskStatus of \n$taskObject ---")
            val currentStatus = getObjectValue(taskObject, "Completed:")
            if(currentStatus != null){
                val completeStatus = currentStatus != "01/01/2000"
                println("_Current Status: $completeStatus _")
                val newStatus = when(completeStatus){
                    false -> LocalDate.now().format(MainActivity.DATE_FORMAT)
                    true -> MainActivity.DEFAULT_DATE
                }
                println("_New Status: ${!completeStatus} _")

                val newTaskContents = taskObject.replace("Completed:$currentStatus", "Completed:$newStatus")
                editEntry(c, fileName, taskObject, newTaskContents)

                //Replace condition of any tasks that have this as a conditions to default None
                if(!completeStatus){
                    val conditionName = getObjectValue(taskObject, "TaskName:")
                    val contextName = getObjectValue(taskObject, "Context:")
                    if(conditionName != null && contextName != null){
                        val allTasksList = readFile(c, MainActivity.TASK_FILE)
                        if(allTasksList != null){
                            var allTaskListStr = allTasksList.joinToString("\n")
                            allTaskListStr = allTaskListStr.replace("Condition:$conditionName","Condition:None")
                            overwriteFile(c,  MainActivity.TASK_FILE, allTaskListStr)
                            return allTaskListStr.split("\n").filter { it.contains("Context:$contextName\t") }
                        }
                    }
                }
                //Return new task list to output into fragment
                val newTaskList = taskList.joinToString("\n").replace(taskObject, newTaskContents).split("\n")
                return newTaskList
            }
            return taskList
        }

        fun sortTaskList(taskList: List<String>): List<String>{
            println("--- sortTaskList of \n$taskList ---")
            //Sort items by completion, condition, start date, due date and then score
            var sortedTaskList = taskList.filter{it.contains("TaskName:")}
            val emptyContextList = taskList.filter{!it.contains("TaskName:")}

            try{
                sortedTaskList = sortedTaskList.sortedWith(nullsLast(compareBy(
                    {
                        //Sort 1. Completed status (incomplete always first)
                        LocalDate.parse(getObjectValue(it, "Completed:"), MainActivity.DATE_FORMAT)
                    },
                    {
                        //Sort 2. Conditional tasks attached (None always first)
                        getObjectValue(it, "Condition:") != "None"
                    },
                    {
                        //Sort 3. Start date is after today
                        LocalDate.parse(getObjectValue(it, "StartDate:"), MainActivity.DATE_FORMAT) > LocalDate.now()
                    },
                    {
                        //Sort 4. Due date (more urgent always first)
                        LocalDate.parse(getObjectValue(it, "DueDate:"), MainActivity.DATE_FORMAT)
                    },
                    {
                        //Sort 5. Complexity (lowest value first)
                        getObjectValue(it, "Complexity:")!!.toInt()
                    },
                    {
                        //Sort 6. Motivation (lowest value first)
                        getObjectValue(it, "Motivation:")!!.toInt()
                    })))
            } catch(e: Exception){
                println("___ Error: $e ___")
            }

            return sortedTaskList + emptyContextList
        }

        fun archiveTasks(c: Context){
            //Check the file contents for expired completed tasks
            val taskList = readFile(c, MainActivity.TASK_FILE)
            println("=== Checking for tasks that can be archived:\n$taskList===")
            if(!taskList.isNullOrEmpty()) {
                val archiveTasks = mutableListOf<String>()
                try{
                    taskList.forEach { task ->
                        if(task.contains("TaskName:")){
                            //First check whether the completed status is now complete and it was not completed today
                            val todayDateStr = LocalDate.now().format(MainActivity.DATE_FORMAT)
                            val completedStr = getObjectValue(task, "Completed:")!!
                            if(completedStr != "01/01/2000" && completedStr != todayDateStr){
                                //Task is expired and will be archived unless it repeats
                                val repeatFlagStr = getObjectValue(task, "Repeat:")!!
                                val frequencyStr = getObjectValue(task, "Frequency:")!!
                                if(repeatFlagStr.toBoolean() && frequencyStr != "None"){
                                    //Wipe the completed status back to default and change start and due date based on frequency
                                    var newTask = task.replace("Completed:$completedStr", "Completed:01/01/2000")
                                    val oldStartDateStr = getObjectValue(task, "StartDate:")!!
                                    val oldDueDateStr = getObjectValue(task, "DueDate:")!!
                                    var startDate = LocalDate.parse(oldStartDateStr, MainActivity.DATE_FORMAT)
                                    var dueDate = LocalDate.parse(oldDueDateStr, MainActivity.DATE_FORMAT)
                                    val todayDate = LocalDate.parse(todayDateStr, MainActivity.DATE_FORMAT)

                                    //Check that new dates are not still before today
                                    while(startDate.isBefore(todayDate)){
                                        startDate = addTimeToDate(frequencyStr, startDate.format(MainActivity.DATE_FORMAT))
                                        dueDate = addTimeToDate(frequencyStr, dueDate.format(MainActivity.DATE_FORMAT))
                                    }

                                    //Convert new dates back into strings and edit entire line
                                    val newStartDateStr = startDate.format(MainActivity.DATE_FORMAT)
                                    val newDueDateStr = dueDate.format(MainActivity.DATE_FORMAT)
                                    newTask = newTask.replace("StartDate:$oldStartDateStr", "StartDate:$newStartDateStr")
                                    newTask = newTask.replace("DueDate:$oldDueDateStr", "DueDate:$newDueDateStr")
                                    editEntry(c, MainActivity.TASK_FILE, task, newTask)
                                }
                                else{
                                    //Add the task item to the archive list
                                    archiveTasks.add(task)
                                }
                            }
                        }
                    }
                }
                catch (e : java.lang.Exception){
                    println("___ Error $e ___")
                    e.stackTrace.forEach {
                        println(it)
                    }
                }

                //Remove all archived tasks from main file and append archive file
                if(archiveTasks.isNotEmpty()){
                    val newTaskListStr = taskList.filter{ !archiveTasks.contains(it) }.joinToString("\n")
                    overwriteFile(c, MainActivity.TASK_FILE, newTaskListStr)
                    appendFile(c, MainActivity.ARCHIVE_FILE, "${archiveTasks.joinToString("\n")}\n")
                }
                else{
                    println("=== No Tasks to archive at this time ===")
                }
            }
        }

        fun unArchiveTasks(c: Context, archiveList :List<String>): List<String>{
            //Check the archive file contents for un completed tasks
            println("=== Checking for tasks that can be unarchived:\n$archiveList===")
            if(archiveList.isNotEmpty()) {
                val uncompletedTasks = mutableListOf<String>()
                try{
                    archiveList.forEach { archivedTask ->
                        if(archivedTask.contains("TaskName:")){
                            //Check whether the completed status is un complete
                            val completedStr = getObjectValue(archivedTask, "Completed:")!!
                            if(completedStr == MainActivity.DEFAULT_DATE){
                                //Task is uncompleted and must return to regular lists
                                uncompletedTasks.add(archivedTask)
                            }
                        }
                    }
                    //Remove all uncompleted tasks from main file and append task file
                    if(uncompletedTasks.isNotEmpty()){
                        val newArchiveList = readFile(c, MainActivity.ARCHIVE_FILE)?.filter{ !uncompletedTasks.contains(it) }
                        if(newArchiveList != null){
                            val newArchiveListStr = newArchiveList.joinToString("\n")
                            overwriteFile(c, MainActivity.ARCHIVE_FILE, newArchiveListStr)
                        }
                        appendFile(c, MainActivity.TASK_FILE, "${uncompletedTasks.joinToString("\n")}\n")

                        return archiveList.filter { !uncompletedTasks.contains(it) }
                    }
                    else{
                        println("=== No Tasks to unarchive at this time ===")
                        return archiveList.toList()
                    }
                }
                catch (e : java.lang.Exception){
                    println("___ Error $e ___")
                    e.stackTrace.forEach {
                        println(it)
                    }
                }


            }
            return listOf("")
        }

        fun transferTasks(c: Context, oldContext : String, newContext: String){
            //Retrieve the original task file and filter based on old context
            val taskReadOutput = readFile(c, MainActivity.TASK_FILE)
            if(taskReadOutput != null){
                val oldContextPointer = "Context:$oldContext\t"
                val newContextPointer = "Context:$newContext\t"
                val newTaskOutput = taskReadOutput.joinToString("\n").replace(oldContextPointer, newContextPointer)
                overwriteFile(c, MainActivity.TASK_FILE, newTaskOutput)
            }
        }

        fun getObjectValue(item:String ,key: String): String?{
            //Determine if key exists in item, if so, extract value
            return if(item.contains(key))
                item.split(key)[1].split("\t")[0]
            else
                null
        }

        fun extractContextList(c: Context): MutableList<String>?{
            val rawContextList = readFile(c, MainActivity.CONTEXT_FILE)
            if(rawContextList != null){
                val rawContextListStr = rawContextList.joinToString("\n")
                var formattedList = rawContextListStr.replace("Name:", "")
                formattedList = formattedList.replace("\t", "")
                return formattedList.split("\n").toMutableList()
            }
            else return null
        }


    }
}