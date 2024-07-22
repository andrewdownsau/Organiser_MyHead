package andrew.organiser.myhead

import andrew.organiser.myhead.databinding.TaskArchiveBinding
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController

class TaskArchive : Fragment() {

    private var _binding: TaskArchiveBinding? = null
    private val standardTaskObjectSize = 14

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TaskArchiveBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("--- Archived Task List ---")
        val fragmentContext = requireContext().applicationContext

        //Extract context file name from fragment bundle
        val archiveFileName = arguments?.getString("archiveFileName")
        println("archiveFileName:$archiveFileName")
        if(!archiveFileName.isNullOrEmpty() && archiveFileName.contains("_")){
            //Set actionbar title
            (activity as AppCompatActivity).supportActionBar?.title = "${archiveFileName.split("_")[1]} Archive"

            //Check file contents, create list if items exist
            val warning = "No tasks archived"
            val readOutput = FileHelper.checkFile(fragmentContext, archiveFileName, warning, standardTaskObjectSize)
            if(readOutput == warning) {
                binding.subtitleTaskText.visibility = View.VISIBLE
                binding.subtitleTaskText.text = readOutput
            }
            else {
                binding.subtitleTaskText.visibility = View.GONE

                //Create list of task buttons from file in order
                val sortedRead = FileHelper.sortTaskList(readOutput)
                UIHelper.createTaskButtonList(fragmentContext, binding.taskListLayout, archiveFileName, sortedRead, findNavController())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}