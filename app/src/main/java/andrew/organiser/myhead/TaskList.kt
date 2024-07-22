package andrew.organiser.myhead

import andrew.organiser.myhead.databinding.TaskListBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskList : Fragment() {

    private var _binding: TaskListBinding? = null
    private val standardTaskObjectSize = 14

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TaskListBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("--- Task List ---")
        val fragmentContext = requireContext().applicationContext

        //Extract context file name from fragment bundle
        var contextFileName = arguments?.getString("contextFileName")
        if(!contextFileName.isNullOrEmpty() && contextFileName.contains("_")){
            //Set actionbar title
            (activity as AppCompatActivity).supportActionBar?.title = "${contextFileName.split("_")[1]} Tasks"

            //Reformat filename into actual text file
            val archiveFileName = "${contextFileName}_archive.txt"
            contextFileName += ".txt"
            println("contextFileName:$contextFileName")

            //Navigate to add edit fragment using plus button
            binding.fabTaskAdd.setOnClickListener {
                findNavController().navigate(R.id.action_TaskList_to_TaskAddEdit, bundleOf("fileName" to contextFileName))
            }

            //Navigate to archive using 2nd fab
            binding.fabTaskArchive.setOnClickListener {
                findNavController().navigate(R.id.action_TaskList_to_TaskArchive, bundleOf("archiveFileName" to archiveFileName))
            }

            //Check file contents, create list if items exist
            val warning = "No tasks listed.\nPlease add content."
            val readOutput = FileHelper.checkFile(fragmentContext, contextFileName, warning, standardTaskObjectSize)
            if(readOutput == warning) {
                binding.subtitleTaskText.visibility = View.VISIBLE
                binding.subtitleTaskText.text = readOutput
            }
            else {
                binding.subtitleTaskText.visibility = View.GONE

                //Check if any tasks should be archived based on time
                FileHelper.archiveTasks(fragmentContext, contextFileName, readOutput)

                //Create list of task buttons from file in order
                val sortedRead = FileHelper.sortTaskList(readOutput)
                UIHelper.createTaskButtonList(fragmentContext, binding.taskListLayout, contextFileName, sortedRead, findNavController())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}