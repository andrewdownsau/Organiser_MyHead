package andrew.organiser.myhead

import andrew.organiser.myhead.databinding.TaskAddEditBinding
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.lang.Exception
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone


class TaskAddEdit : Fragment() {

    private var _binding: TaskAddEditBinding? = null
    private val standardTaskObjectSize = 14

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TaskAddEditBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("---Task Add/Edit ---")
        val fragmentContext = requireContext().applicationContext

        //Close button redirect
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        //Extract context file name from bundle, cannot do anything without
        val contextFileName = arguments?.getString("fileName")
        if(!contextFileName.isNullOrEmpty()){
            //Setting all aspects of task if in edit mode
            val taskContent = arguments?.getString("contentLine")

            //Populating frequencies in spinner as required
            val freqSpinnerList = mutableListOf("None", "Daily", "Weekly", "Fortnightly", "Monthly", "Quarterly", "Yearly")
            val adp1: ArrayAdapter<String> = ArrayAdapter<String>(fragmentContext, R.layout.spinner_item, freqSpinnerList)
            adp1.setDropDownViewResource(R.layout.spinner_item)
            binding.frequencySpinner.setAdapter(adp1)

            //Populating conditions in spinner as required
            val conditionSpinnerList = FileHelper.getConditionList(fragmentContext, contextFileName, taskContent)
            val adp2: ArrayAdapter<String> = ArrayAdapter<String>(fragmentContext, R.layout.spinner_item, conditionSpinnerList)
            adp2.setDropDownViewResource(R.layout.spinner_item)
            binding.conditionSpinner.setAdapter(adp2)

            //Set and change seekbar values based on current position
            setOnSeekbarChangeListener(binding.seekbarComplexity, binding.complexityValue)
            setOnSeekbarChangeListener(binding.seekbarPositiveMotivation, binding.positiveMotivationValue)
            setOnSeekbarChangeListener(binding.seekbarNegativeMotivation, binding.negativeMotivationValue)
            setOverallScore()

            //Set DatePicker dialogue
            setDateListener(binding.editStartDate)
            setDateListener(binding.editEndDate)

            //Disable frequency spinner based on current or changed state of checkbox
            binding.frequencySpinner.isEnabled = binding.repeatCheck.isChecked
            binding.repeatCheck.setOnCheckedChangeListener { _, isChecked ->
                binding.frequencySpinner.isEnabled = isChecked
            }

            // --- Edit Task Mode ---
            if(!taskContent.isNullOrEmpty()){
                //Extract all values from content and populate to widgets
                val taskObject = taskContent.split(";")
                try{
                    binding.editTaskName.setText(taskObject[1])
                    binding.editMotiveName.setText(taskObject[2])
                    binding.seekbarComplexity.progress = taskObject[3].toInt()
                    binding.seekbarPositiveMotivation.progress = taskObject[4].toInt()
                    binding.seekbarNegativeMotivation.progress = taskObject[5].toInt()
                    binding.overallValue.text = taskObject[6]
                    binding.editStartDate.setText(taskObject[7])
                    binding.editEndDate.setText(taskObject[8])
                    binding.repeatCheck.isChecked = taskObject[9].toBoolean()
                    binding.frequencySpinner.post {
                        binding.frequencySpinner.setSelection(adp1.getPosition(taskObject[10]))
                    }
                    binding.conditionSpinner.post {
                        binding.conditionSpinner.setSelection(adp2.getPosition(taskObject[11]))
                    }
                    binding.editNotes.setText(taskObject[12])

                }catch (e: Exception){
                    println("___ Error: $e ___")
                }
                //Save button file write and redirect
                binding.btnSave.setOnClickListener {
                    val taskId = taskObject[0]
                    val newLineContent = "${taskId};${saveContent()};${taskObject.last()}"
                    FileHelper.editEntry(fragmentContext, contextFileName, taskId.toInt(), newLineContent, standardTaskObjectSize )

                    findNavController().popBackStack()
                }

                //Delete button deletes entry
                binding.btnDelete.setOnClickListener {
                    FileHelper.deleteEntry(requireContext().applicationContext, contextFileName, taskObject[0].toInt())
                    findNavController().popBackStack()
                }
            }

            // --- Create New Task Mode ---
            else{
                //Set Default values for start and due date
                val defaultDate = LocalDate.now().format(FileHelper.dateTimeFormatter)
                binding.editStartDate.setText(defaultDate)
                binding.editEndDate.setText(defaultDate)

                //Save button file write and redirect
                binding.btnSave.setOnClickListener {
                    val newLineContent = saveContent() + ";01/01/2000"
                    FileHelper.createNewEntry(fragmentContext, contextFileName, newLineContent)

                    findNavController().popBackStack()
                }
                //Delete button is disabled and has no onClick function
                binding.btnDelete.setBackgroundResource(R.drawable.shadow_button_disabled)
                binding.btnDelete.setTextColor(Color.parseColor("#555555"))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveContent() : String {
        val newLineContent  = mutableListOf<String>()
        newLineContent.add(binding.editTaskName.text.toString())
        newLineContent.add(binding.editMotiveName.text.toString())
        newLineContent.add(binding.seekbarComplexity.progress.toString())
        newLineContent.add(binding.seekbarPositiveMotivation.progress.toString())
        newLineContent.add(binding.seekbarNegativeMotivation.progress.toString())
        newLineContent.add(getOverallScore().toString())
        newLineContent.add(binding.editStartDate.text.toString())
        newLineContent.add(binding.editEndDate.text.toString())
        newLineContent.add(binding.repeatCheck.isChecked.toString())
        newLineContent.add(binding.frequencySpinner.selectedItem.toString())
        newLineContent.add(binding.conditionSpinner.selectedItem.toString())
        newLineContent.add(binding.editNotes.text.toString())
        return newLineContent.joinToString(";")
    }

    private fun setOnSeekbarChangeListener(seekBar: SeekBar, valueText: TextView){
        //Set value text progress amount as a string in UI
        var seekbarProgress = seekBar.progress + 1
        valueText.text = seekbarProgress.toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // React to the value being set in seekBar
                seekbarProgress = progress + 1
                valueText.text = seekbarProgress.toString()
                setOverallScore()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }

            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })
    }

    private fun setOverallScore(){
        //Use score to set progress bar value
        val overallScore = getOverallScore()
        binding.overallValue.text = overallScore.toString()
        binding.overallProgressBarPos.progress = overallScore
        binding.overallProgressBarNeg.progress = overallScore * -1
    }

    private fun getOverallScore() : Int{
        //Use values of each bar to calculate the overall score
        val complexVal = binding.complexityValue.text.toString().toInt()
        val posMotVal = binding.positiveMotivationValue.text.toString().toInt()
        val negMotVal = binding.negativeMotivationValue.text.toString().toInt()

        return posMotVal - complexVal - negMotVal
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

    private fun validateDate(checkDateEdit: EditText, checkDateStr: String){
        //Check date order based on flag
        println("--- ValidateDate TaskAddEdit ---")
        try{
            var otherDate: LocalDate? = null
            val checkDate = LocalDate.parse(checkDateStr, FileHelper.dateTimeFormatter)
            var dateValid = true
            if(checkDateEdit == binding.editEndDate){
                otherDate = LocalDate.parse(binding.editStartDate.text, FileHelper.dateTimeFormatter)
                dateValid = !otherDate.isAfter(checkDate)
            }
            if(checkDateEdit == binding.editStartDate){
                otherDate = LocalDate.parse(binding.editEndDate.text, FileHelper.dateTimeFormatter)
                dateValid = !otherDate.isBefore(checkDate)
            }

            //Debug Logs
            println("_otherDate:$otherDate")
            println("_checkDate:$checkDate")

            if(dateValid){
                binding.editStartDate.setTextColor(Color.parseColor("#8694B1"))
                binding.editEndDate.setTextColor(Color.parseColor("#8694B1"))
            }
            else{
                binding.editStartDate.setTextColor(Color.parseColor("#ff0000"))
                binding.editEndDate.setTextColor(Color.parseColor("#ff0000"))
            }

        }catch (e: Exception){
            println("___ Error: $e ___")
        }
    }

}