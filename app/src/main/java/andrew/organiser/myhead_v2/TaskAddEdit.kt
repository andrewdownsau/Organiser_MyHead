package andrew.organiser.myhead_v2

import andrew.organiser.myhead_v2.databinding.TaskAddEditBinding
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.lang.Exception
import java.time.LocalDate
import java.time.chrono.MinguoDate
import java.util.Calendar
import java.util.TimeZone

class TaskAddEdit : Fragment() {

    private var _binding: TaskAddEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var datesValid = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TaskAddEditBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentContext = requireContext().applicationContext

        //Check if any tasks should be archived based on time
        FileHelper.archiveTasks(fragmentContext)

        //Close button redirect
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        //Extract context file name from bundle, cannot do anything without
        val contextObject = arguments?.getString("contextObject")
        if (contextObject != null) {
            val contextName = FileHelper.getObjectValue(contextObject, "Name:")
            val contextList = FileHelper.extractContextList(fragmentContext)
            if(contextName != null && contextList != null){
                println("---Task Add/Edit from context: $contextName ---")
                val taskObject = arguments?.getString("taskObject")

                //Populate spinners with required values
                val conditionList = FileHelper.getConditionList(fragmentContext, contextName, taskObject)
                val adapterList = populateSpinners(fragmentContext, contextList, conditionList, contextName)

                //Set and change seekbar values based on current position
                setOnSeekbarChangeListener(binding.seekbarComplexity, binding.complexityValue)
                setOnSeekbarChangeListener(binding.seekbarMotivation, binding.motivationValue)

                //Set DatePicker dialogue and frequency spinner based on repeat flag
                setDateListener(binding.editStartDate)
                setDateListener(binding.editEndDate)
                setRepeatListener(binding.repeatCheck)

                // --- Create new mode if taskObject is null --- //
                if(taskObject == null){
                    //Set Default values for start and due date
                    val defaultDate = LocalDate.now().format(MainActivity.DATE_FORMAT)
                    binding.editStartDate.setText(defaultDate)
                    binding.editEndDate.setText(defaultDate)

                    //Save button file write and redirect
                    binding.btnSave.setOnClickListener {
                        val newTaskName = "TaskName:${binding.editTaskName.text}\t"
                        var newTask = saveContent() + "Completed:01/01/2000\t"
                        val validWarning = FileHelper.validateEntry(fragmentContext, "", newTaskName, MainActivity.TASK_FILE)
                        if(validWarning == "valid" && datesValid){
                            newTask += "\n"
                            FileHelper.appendFile(fragmentContext, MainActivity.TASK_FILE, newTask)
                            findNavController().popBackStack()
                        }
                        else {
                            binding.labelWarning.visibility = View.VISIBLE
                            binding.labelWarning.text = validWarning

                            if(!datesValid)
                                binding.labelWarning.text = resources.getString(R.string.date_warning)
                        }
                    }
                    //Delete button is disabled and has no onClick function
                    binding.btnDelete.setBackgroundResource(R.drawable.shadow_button_disabled)
                    binding.btnDelete.setTextColor(Color.parseColor("#555555"))
                }
                // --- Edit task mode if taskObject is not null --- //
                else{
                    //Populate the values from the task object
                    try{
                        binding.contextSpinner.post {
                            binding.contextSpinner.setSelection(adapterList[0].getPosition(FileHelper.getObjectValue(taskObject, "Context:")))
                        }
                        binding.editTaskName.setText(FileHelper.getObjectValue(taskObject, "TaskName:"))
                        binding.editMotiveName.setText(FileHelper.getObjectValue(taskObject, "TaskMotive:"))
                        binding.seekbarComplexity.progress = FileHelper.getObjectValue(taskObject, "Complexity:")!!.toInt()
                        binding.editStartDate.setText(FileHelper.getObjectValue(taskObject, "StartDate:"))
                        binding.editEndDate.setText(FileHelper.getObjectValue(taskObject, "DueDate:"))
                        binding.repeatCheck.isChecked = FileHelper.getObjectValue(taskObject, "Repeat:").toBoolean()
                        binding.frequencySpinner.post {
                            binding.frequencySpinner.setSelection(adapterList[1].getPosition(FileHelper.getObjectValue(taskObject, "Frequency:")))
                        }
                        binding.conditionSpinner.post {
                            binding.conditionSpinner.setSelection(adapterList[2].getPosition(FileHelper.getObjectValue(taskObject, "Condition:")))
                        }
                        binding.editNotes.setText(FileHelper.getObjectValue(taskObject, "Notes:"))
                        binding.seekbarMotivation.progress = FileHelper.getObjectValue(taskObject, "Motivation:")!!.toInt()

                    }catch (e: Exception){
                        println("___ Error: $e ___")
                    }
                    //Save button file write and redirect
                    binding.btnSave.setOnClickListener {
                        val oldTaskName = "TaskName:${FileHelper.getObjectValue(taskObject, "TaskName:")}\t"
                        val newTaskName = "TaskName:${binding.editTaskName.text}\t"
                        val newTask = saveContent() + "Completed:${FileHelper.getObjectValue(taskObject, "Completed:")}\t"
                        val validWarning = FileHelper.validateEntry(fragmentContext, oldTaskName, newTaskName, MainActivity.TASK_FILE)
                        if(validWarning == "valid" && datesValid){
                            //Save edited entry to context list
                            FileHelper.editEntry(fragmentContext, MainActivity.TASK_FILE, taskObject, newTask )
                            findNavController().popBackStack()
                        }
                        else {
                            binding.labelWarning.visibility = View.VISIBLE
                            binding.labelWarning.text = validWarning

                            if(!datesValid)
                                binding.labelWarning.text = resources.getString(R.string.date_warning)
                        }
                    }

                    //Delete button deletes entry
                    binding.btnDelete.setOnClickListener {
                        FileHelper.deleteEntry(requireContext().applicationContext, MainActivity.TASK_FILE, taskObject)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //--- Setting Population for Spinners --- //
    private fun populateSpinners(c: Context, contextList: MutableList<String>, conditionList: MutableList<String>, contextName: String) : List<ArrayAdapter<String>>{
        //Clear any list items that are empty
        val checkedContextList = contextList.filter { it.isNotEmpty() }
        val checkedConditionList = conditionList.filter { it.isNotEmpty() }
        val frequencyList = mutableListOf("None", "Daily", "Weekly", "Fortnightly", "Monthly", "Quarterly", "Yearly")

        val adp1 = ArrayAdapter(c, R.layout.spinner_item, checkedContextList)
        adp1.setDropDownViewResource(R.layout.spinner_item)
        binding.contextSpinner.setAdapter(adp1)
        binding.contextSpinner.post {
            binding.contextSpinner.setSelection(adp1.getPosition(contextName))
        }

        val adp2 = ArrayAdapter(c, R.layout.spinner_item, frequencyList)
        adp2.setDropDownViewResource(R.layout.spinner_item)
        binding.frequencySpinner.setAdapter(adp2)

        val adp3 = ArrayAdapter(c, R.layout.spinner_item, checkedConditionList)
        adp3.setDropDownViewResource(R.layout.spinner_item)
        binding.conditionSpinner.setAdapter(adp3)

        return listOf(adp1, adp2, adp3)
    }


    // --- Setting Listeners for seekbars and repeat check --- //
    private fun setOnSeekbarChangeListener(seekBar: SeekBar, valueText: TextView){
        //Set value text progress amount as a string in UI
        var seekbarProgress = seekBar.progress + 1
        valueText.text = seekbarProgress.toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // React to the value being set in seekBar
                seekbarProgress = progress + 1
                valueText.text = seekbarProgress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }

            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })
    }

    private fun setDateListener(editText: EditText){
        editText.setOnClickListener {

            val calendar: Calendar = Calendar.getInstance(TimeZone.getDefault())

            val dialog = DatePickerDialog(
                requireActivity(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    //Set the text to include 2 digits for month and day
                    val actualMonth = selectedMonth + 1
                    val dayText = if(selectedDay < 10) "0$selectedDay" else selectedDay.toString()
                    val monthText = if(actualMonth < 10) "0$actualMonth" else actualMonth.toString()
                    val displayedDate = "$dayText/$monthText/$selectedYear"

                    //Check start date if end date and do not accept any date before start
                    validateDate(editText, displayedDate)
                    editText.setText(displayedDate)
                },
                calendar[Calendar.YEAR], calendar[Calendar.MONTH],
                calendar[Calendar.DAY_OF_MONTH]
            )
            dialog.show()
        }
    }

    private fun setRepeatListener(checkBox: CheckBox){
        //Disable frequency spinner based on current or changed state of checkbox
        if(checkBox.isChecked) {
            binding.frequencySpinner.visibility = View.VISIBLE
            binding.labelFrequency.visibility = View.VISIBLE
        }
        else {
            binding.frequencySpinner.visibility = View.GONE
            binding.labelFrequency.visibility = View.GONE
        }

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                binding.frequencySpinner.visibility = View.VISIBLE
                binding.labelFrequency.visibility = View.VISIBLE
            }
            else {
                binding.frequencySpinner.visibility = View.GONE
                binding.labelFrequency.visibility = View.GONE
            }
        }
    }

    private fun validateDate(checkDateEdit: EditText, checkDateStr: String){
        //Check date order based on flag
        println("--- ValidateDate TaskAddEdit ---")
        try{
            var otherDate: LocalDate? = null
            val checkDate = LocalDate.parse(checkDateStr, MainActivity.DATE_FORMAT)
            var dateValid = true
            if(checkDateEdit == binding.editEndDate){
                otherDate = LocalDate.parse(binding.editStartDate.text, MainActivity.DATE_FORMAT)
                dateValid = !otherDate.isAfter(checkDate)
            }
            else if(checkDateEdit == binding.editStartDate){
                otherDate = LocalDate.parse(binding.editEndDate.text, MainActivity.DATE_FORMAT)
                dateValid = !otherDate.isBefore(checkDate)
            }

            //Debug Logs
            println("_otherDate:$otherDate")
            println("_checkDate:$checkDate")
            println("_dateValid:$dateValid")

            if(dateValid){
                binding.editStartDate.setTextColor(Color.parseColor("#8694B1"))
                binding.editEndDate.setTextColor(Color.parseColor("#8694B1"))
            }
            else{
                binding.editStartDate.setTextColor(Color.parseColor("#ff0000"))
                binding.editEndDate.setTextColor(Color.parseColor("#ff0000"))
            }

            datesValid = dateValid
        }catch (e: Exception){
            println("___ Error: $e ___")
        }
    }

    private fun saveContent() : String {
        var newTaskContentStr  = ""
        newTaskContentStr += "Context:${binding.contextSpinner.selectedItem}\t"
        newTaskContentStr += "TaskName:${binding.editTaskName.text}\t"
        newTaskContentStr += "TaskMotive:${binding.editMotiveName.text}\t"
        newTaskContentStr += "Complexity:${binding.seekbarComplexity.progress}\t"
        newTaskContentStr += "Motivation:${binding.seekbarMotivation.progress}\t"
        newTaskContentStr += "StartDate:${binding.editStartDate.text}\t"
        newTaskContentStr += "DueDate:${binding.editEndDate.text}\t"
        newTaskContentStr += "Repeat:${binding.repeatCheck.isChecked}\t"
        newTaskContentStr += "Frequency:${binding.frequencySpinner.selectedItem}\t"
        newTaskContentStr += "Condition:${binding.conditionSpinner.selectedItem}\t"
        newTaskContentStr += "Notes:${binding.editNotes.text}\t"
        return newTaskContentStr
    }

}