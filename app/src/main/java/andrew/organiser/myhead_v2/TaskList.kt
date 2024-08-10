package andrew.organiser.myhead_v2

import andrew.organiser.myhead_v2.databinding.TaskListBinding
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class TaskList : Fragment() {

    private var _binding: TaskListBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TaskListBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onStart() {
        super.onStart()
        val fragmentContext = requireContext().applicationContext
        //FileHelper.deleteEntry(fragmentContext, MainActivity.TASK_FILE, "All")
        generateTaskList(fragmentContext)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun generateTaskList(c: Context){
        //Check if any tasks should be archived based on time
        FileHelper.archiveTasks(c)

        //Extract context file name from fragment bundle
        val contextObject = arguments?.getString("contextObject")
        if(contextObject != null){
            println("--- Task List for: $contextObject ---")
            val contextBundle = bundleOf("contextObject" to contextObject)

            //Extract context name from object for task search
            val contextName = FileHelper.getObjectValue(contextObject, "Name:")
            if(contextName != null) {
                //Set actionbar title
                (activity as AppCompatActivity).supportActionBar?.title = "$contextName Tasks"

                //Navigate to add edit fragment using plus button
                binding.fabTaskAdd.setOnClickListener {
                    findNavController().navigate(R.id.action_TaskList_to_TaskAddEdit, contextBundle)
                }

                //Navigate to archive using 2nd fab
                binding.fabTaskArchive.setOnClickListener {
                    findNavController().navigate(R.id.action_TaskList_to_TaskArchive, contextBundle)
                }

                //Check file contents, create list if items exist
                val readOutput = FileHelper.readFile(c, MainActivity.TASK_FILE)
                if(readOutput != null){
                    val tasksInContext = readOutput.filter { it.contains("Context:$contextName\t") }
                    if(tasksInContext.isEmpty())
                        binding.subtitleTaskText.visibility = View.VISIBLE
                    else {
                        binding.subtitleTaskText.visibility = View.GONE

                        //Create list of task buttons from file in order
                        val sortedTasksInContext = FileHelper.sortTaskList(tasksInContext)
                        UIHelper.createTaskButtonList(c, binding.taskListLayout, sortedTasksInContext, findNavController())
                    }
                }
                else{
                    binding.subtitleTaskText.visibility = View.VISIBLE
                }
            }
        }
    }
}