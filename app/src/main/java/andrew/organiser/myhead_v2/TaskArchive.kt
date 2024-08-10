package andrew.organiser.myhead_v2

import andrew.organiser.myhead_v2.databinding.TaskArchiveBinding
import android.content.Context
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val fragmentContext = requireContext().applicationContext
        generateArchiveList(fragmentContext)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentContext = requireContext().applicationContext
        //FileHelper.deleteEntry(fragmentContext, MainActivity.ARCHIVE_FILE, "All")

        //Generate tasks related to context
        generateArchiveList(fragmentContext)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun generateArchiveList(c: Context){
        //Check if any tasks should be archived based on time
        FileHelper.archiveTasks(c)

        //Extract context file name from fragment bundle
        val contextObject = arguments?.getString("contextObject")
        if(contextObject != null){
            val contextName = FileHelper.getObjectValue(contextObject, "Name:")
            if(contextName != null){
                //Set actionbar title
                println("--- Archived $contextName Task List ---")
                (activity as AppCompatActivity).supportActionBar?.title = "$contextName Archive"

                //Check archive contents, create list if items exist
                val archiveList = FileHelper.readFile(c, MainActivity.ARCHIVE_FILE)
                if(!archiveList.isNullOrEmpty()) {
                    val archivedInContext = archiveList.filter { task -> task.contains("Context:$contextName") }
                    if(archivedInContext.isEmpty()) {
                        binding.subtitleTaskText.visibility = View.VISIBLE
                    }
                    else {
                        binding.subtitleTaskText.visibility = View.GONE

                        //Create list of task buttons from file in order
                        UIHelper.createTaskButtonList(c, binding.taskListLayout, archivedInContext, findNavController())
                    }
                }
            }
        }
    }
}