package andrew.organiser.myhead_v2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import andrew.organiser.myhead_v2.databinding.UrgentListBinding
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate

/**
 * Context List for organiser, navigates by default and redirects if context check result is valid
 */
class UrgentList : Fragment() {

    private var _binding: UrgentListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = UrgentListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("--- Urgent List: onViewCreated ---")
        super.onViewCreated(view, savedInstanceState)
        val fragmentContext = requireContext().applicationContext

        //Check if any tasks should be archived based on time
        FileHelper.archiveTasks(fragmentContext)

        //Check task file, create list if urgent items if they exist
        val readOutput = FileHelper.readFile(fragmentContext, MainActivity.TASK_FILE)
        if(readOutput != null) {
            try{
                val todayDate = MainActivity.SIMPLE_DF.parse(LocalDate.now().format(MainActivity.DATE_FORMAT))!!.toInstant()
                val urgentTasks = mutableListOf<String>()
                readOutput.forEach { task ->
                    val startDateStr = FileHelper.getObjectValue(task, "StartDate:")
                    val dueDateStr = FileHelper.getObjectValue(task, "DueDate:")
                    val conditionStr = FileHelper.getObjectValue(task, "Condition:")
                    val completedStr = FileHelper.getObjectValue(task, "Completed:")
                    if(startDateStr != null && dueDateStr != null && conditionStr != null && completedStr != null) {
                        val startDate = MainActivity.SIMPLE_DF.parse(startDateStr)?.toInstant()
                        val dueDate = MainActivity.SIMPLE_DF.parse(dueDateStr)?.toInstant()
                        val daysFromDueDate = Duration.between(todayDate, dueDate).toDays()
                        println("_daysFromDueDate: $daysFromDueDate")
                        if(todayDate >= startDate && daysFromDueDate < 4 && conditionStr == "None" && completedStr == "01/01/2000")
                            urgentTasks.add(task)
                    }
                }
                println("_urgentTasks: $urgentTasks")
                if(urgentTasks.isEmpty()) {
                    binding.subtitleText.visibility = View.VISIBLE
                }
                else {
                    binding.subtitleText.visibility = View.GONE
                    //Create list of urgent task buttons from file in order
                    val sortedUrgentList = FileHelper.sortTaskList(urgentTasks)
                    UIHelper.createTaskButtonList(fragmentContext, binding.urgentListLayout, sortedUrgentList, findNavController())
                }
            }
            catch (e : Exception){
                println("___ Error: $e ___")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}