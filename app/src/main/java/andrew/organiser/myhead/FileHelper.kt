package andrew.organiser.myhead

import android.content.Context
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class FileHelper {
    companion object {
        private const val STANDARD_TASK_OBJECT_SIZE = 14
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        private fun readFile(c: Context, fileName: String) : String? {
            var content: String? = null
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
            return content
        }

        private fun appendFile(c: Context, fileName: String, content: String){
            println("--- appendFileContents: $content")
            c.openFileOutput(fileName, Context.MODE_APPEND).use {
                it.write(content.toByteArray())
            }
        }

        private fun overwriteFile(c: Context, fileName: String, content: String){
            println("--- overwriteFileContents: $content ---")
            c.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        }

        private fun validateLineCheck(contentLine: String?, condition: Int) : Boolean{
            println("_Validation Line Check: $contentLine _")
            if(!contentLine.isNullOrEmpty()){
                val lineSize = contentLine.split(";").size
                //println("_lineSize: $lineSize _")
                //println("_condition: $condition _")
                try{
                    contentLine.split(";")[0].toInt()
                } catch (e: Exception){
                    println("___ Error: $e ___")
                    return false
                }

                return lineSize == condition
            }

            return true
        }

        private fun validateContentCheck(fileContent: String?, condition: Int) : String{
            var newFileContent = ""
            if (fileContent != null) {
                println("_Validation File Check: $fileContent _")
                for(line in fileContent.split("\n")){
                    if(line.isNotEmpty()){
                        if(!validateLineCheck(line, condition)) {
                            //Delete invalid lines from file by not adding
                            println("___ Invalid Line: $line ___")
                        }
                        else
                            newFileContent += "\n$line"
                    }
                }
            } else {
                println("___ Error: fileContent is null ___")
            }
            return newFileContent
        }

        private fun extractLastItemId(contentLines: String?) : Int{
            println("~~~ extractLastItemId ~~~")
            var extractedId = 0
            if(!contentLines.isNullOrEmpty()){
                val lineList = contentLines.split("\n")
                val isolatedLine = lineList[lineList.size - 1]

                if(isolatedLine.isNotEmpty()){
                    try{
                        val isolatedLineList = isolatedLine.split(";")
                        extractedId = isolatedLineList[0].toInt()
                    }catch (e:Exception){
                        println("___ Error: $e ___")
                    }
                }
            }

            println("_extractedId:$extractedId")
            return extractedId
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
            var date = LocalDate.parse(originalDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            date = date.plusDays(dayMovement.toLong())
            date = date.plusMonths(monthMovement.toLong())

            return date
        }

        fun checkFile(c: Context, fileName: String, warning: String, validLength: Int): String {
            println("+++ checkFile: $fileName +++")

            //Validate all content and log
            var readContent = readFile(c, fileName)
            val originalContent = readContent
            if(readContent.isNullOrEmpty()) {
                readContent = warning
            }
            else if(readContent != warning){
                readContent = validateContentCheck(readContent, validLength)
            }

            //Overwrite the file if there are any changes
            if(readContent != originalContent)
                overwriteFile(c, fileName, readContent)

            println("checkFile readContent:$readContent")
            return readContent
        }

        fun createNewEntry(c: Context, fileName: String, contentStr: String){
            println("+++ createNewEntry FileHelper +++")

            //Read previous id value to create current id (start at 1 if empty)
            val readOutput = readFile(c, fileName)
            val nextId = extractLastItemId(readOutput) + 1

            //Format for entry is "\n$nextId;$contentString"
            appendFile(c, fileName, "\n$nextId;$contentStr")

        }

        fun deleteEntry(c: Context, fileName: String, id: Int){
            println("+++ deleteEntry FileHelper +++")
            //overwriteFile(context, fileName, "")

            //Read entire file and remove line with matching Id
            val fileContents = readFile(c, fileName)
            if(!fileContents.isNullOrEmpty()){
                val lineContent = getItemById(c, fileName, id)
                val newFileContents = fileContents.replace("\n$lineContent", "")
                overwriteFile(c, fileName, newFileContents)
            }
        }

        fun editEntry(c: Context, fileName: String, lineId : Int, newLineContents: String, validLength: Int) : String{
            //Debugging Logs
            println("+++ editEntry File: $fileName +++")
            var newFileContents = ""
            val oldLineContents = getItemById(c, fileName, lineId)
            if(!oldLineContents.isNullOrEmpty()){
                println("oldLineContents:$oldLineContents")
                println("newLineContents:$newLineContents")

                val fileContents = readFile(c, fileName)
                if(!fileContents.isNullOrEmpty()){
                    //Validate the edited data before execution of operation
                    if(validateLineCheck(newLineContents, validLength)){
                        newFileContents = fileContents.replace(oldLineContents, newLineContents)
                        overwriteFile(c, fileName, newFileContents)
                    }
                }
            }

            return newFileContents
        }

        fun getItemById(c: Context, fileName: String, id: Int): String? {
            //Read all of contents of file and extract line with correct id
            var returnItem: String? = null
            val fileContents = readFile(c, fileName)
            if(!fileContents.isNullOrEmpty()){
                returnItem = fileContents.drop(fileContents.indexOf("\n$id") + 1)

                //Determine whether the item is last or not by existence of \n
                if(returnItem.contains("\n"))
                    returnItem = returnItem.take(returnItem.indexOf("\n"))
            }
            println("getItemById[$id]: $returnItem")
            return returnItem
        }

        fun getConditionList(c: Context, fileName: String, contentLine: String?): MutableList<String>{
            //Read file and remove content line
            var readOutput = readFile(c, fileName)
            var pendingTasks = "None"
            if (readOutput != null){
                if(!contentLine.isNullOrEmpty() && readOutput.contains(contentLine)) {
                    readOutput = readOutput.replace("\n$contentLine", "")
                }
                //Isolate items that have not been completed
                readOutput.split("\n").forEach {
                    if(it.isNotEmpty() && it.contains(";")){
                        val taskObject = it.split(";")

                        //Check it is a pending task
                        if(taskObject.last() == "01/01/2000") {
                            //Add condition in correct format: ' id - Name'
                            pendingTasks += "\n${taskObject[0]} - ${taskObject[1]}"
                        }
                    }
                }
            }
            println("_Get condition list output: \n$pendingTasks")
            return pendingTasks.split("\n").toMutableList()
        }

        fun changeTaskStatus(c: Context, fileName: String, contentLine: String) : String{
            //Change status of item based on current value
            println("--- changeTaskStatus of \n$contentLine ---")
            val taskObject = contentLine.split(";").toMutableList()
            val currentStatus = taskObject[13]
            val completeStatus = currentStatus != "01/01/2000"
            println("_Current Status: $completeStatus _")
            val newStatus = when(completeStatus){
                false -> LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                true -> "01/01/2000"
            }
            println("_New Status: ${!completeStatus} _")
            taskObject[13] = newStatus
            val newLineContents = taskObject.joinToString(";")
            var newFileContents = editEntry(c, fileName, taskObject[0].toInt(), newLineContents, STANDARD_TASK_OBJECT_SIZE)

            //Replace condition of any tasks that have this as a conditions to default None
            val contentCondition = "${taskObject[0]} - ${taskObject[1]}"
            if(newFileContents.contains(contentCondition)){
                newFileContents = newFileContents.replace(contentCondition, "None")
                overwriteFile(c, fileName, newFileContents)
            }

            return newFileContents
        }

        fun sortTaskList(fileContent: String) :String{
            //Sort items by completion, condition, start date, due date and then score
            var splitRead = fileContent.split("\n").drop(1)

            //Drop any split reads that are empty into own category
            val invalidLines = splitRead.filter { !it.contains(";") }.toList()
            splitRead = splitRead.filter { it.contains(";") }.toList()

            try{
                splitRead = splitRead.sortedWith(nullsLast(compareBy(
                    { LocalDate.parse(it.split(";").last(), dateTimeFormatter) },
                    { it.split(";")[11] != "None" },
                    { LocalDate.parse(it.split(";")[7], dateTimeFormatter) > LocalDate.now() },
                    { LocalDate.parse(it.split(";")[8], dateTimeFormatter) },
                    { it.split(";")[6] })))
            } catch(e: Exception){
                println("___ Error: $e ___")
            }

            //Return format based on presence of invalid lines
            var returnStr = splitRead.joinToString("\n")
            if(invalidLines.isNotEmpty())
                returnStr += "\n${invalidLines.joinToString("\n")}"

            return returnStr
        }

        fun archiveTasks(c: Context, fileName: String, fileContent: String) :String{
            //Check the file contents for expired completed tasks
            val taskList = fileContent.split("\n").drop(1)
            var archiveListStr = ""
            taskList.forEach {
                val taskObject = it.split(";").toMutableList()
                val todayDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                if(taskObject.last() != "01/01/2000" && taskObject.last() != todayDateStr){
                    //Task is expired and will be archived unless it repeats
                    if(taskObject[9].toBoolean() && taskObject[10] != "None"){
                        //Wipe the completed status back to default and change start and due date based on frequency
                        taskObject[13] = "01/01/2000"
                        var startDate = LocalDate.parse(taskObject[7], dateTimeFormatter)
                        var dueDate = LocalDate.parse(taskObject[8], dateTimeFormatter)

                        //Check that new dates are not still before today
                        while(startDate.isBefore(LocalDate.now())){
                            startDate = addTimeToDate(taskObject[10], startDate.toString())
                            dueDate = addTimeToDate(taskObject[10], dueDate.toString())
                        }

                        //Convert new dates back into strings and edit entire line
                        taskObject[7] = startDate.format(dateTimeFormatter)
                        taskObject[8] = dueDate.format(dateTimeFormatter)
                        editEntry(c, fileName, taskObject[0].toInt(), taskObject.joinToString(";"), STANDARD_TASK_OBJECT_SIZE)
                    }
                    else archiveListStr += "\n$it"
                }
            }

            //Remove all archived tasks from main file and append archive file
            var newFileContent = fileContent
            if(archiveListStr.isNotEmpty()){
                newFileContent = fileContent.replace(archiveListStr, "")
                overwriteFile(c, fileName, newFileContent)
                val archiveFileName = fileName.replace(".txt", "_archive.txt")
                appendFile(c, archiveFileName, archiveListStr)
            }
            return newFileContent
        }

        fun transferTasks(c: Context, oldContext : String, newContext: String){
            //Convert contexts into correct file format
            val oldFileName = "${oldContext.replace(";", "_")}.txt"
            val newFileName = "${newContext.replace(";", "_")}.txt"

            //Read old file, delete contents to old file
            val fileContents = readFile(c, oldFileName)
            overwriteFile(c, oldFileName, "")

            //Create new file with old contents
            if (fileContents != null) {
                overwriteFile(c, newFileName, fileContents)
            }

        }
    }
}