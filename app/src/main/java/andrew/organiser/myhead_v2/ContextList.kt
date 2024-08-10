package andrew.organiser.myhead_v2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import andrew.organiser.myhead_v2.databinding.ContextListBinding
import android.content.Context
import androidx.core.os.bundleOf

/**
 * Context List for organiser, navigates by default and redirects if context check result is valid
 */
class ContextList : Fragment() {

    private var _binding: ContextListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContextListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onStart() {
        super.onStart()
        val fragmentContext = requireContext().applicationContext
        generateContextList(fragmentContext)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("--- Context List: onViewCreated ---")
        super.onViewCreated(view, savedInstanceState)
        val fragmentContext = requireContext().applicationContext
        //FileHelper.deleteEntry(fragmentContext, MainActivity.CONTEXT_FILE, "All")

        //Navigate to add/edit fragment using plus button
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_ContextList_to_ContextAddEdit)
        }

        //Navigate to urgent list using 2nd fab
        binding.fabUrgentList.setOnClickListener {
            findNavController().navigate(R.id.action_ContextList_to_UrgentList)
        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun generateContextList(c: Context){
        //Check if any tasks should be archived based on time
        FileHelper.archiveTasks(c)

        //Check context file, create list if items exist
        val readOutput = FileHelper.readFile(c, MainActivity.CONTEXT_FILE)
        if(readOutput == null) {
            binding.subtitleText.visibility = View.VISIBLE
            binding.subtitleText.text = resources.getString(R.string.context_warning)
        }
        else {
            binding.subtitleText.visibility = View.GONE
            //Create list of context buttons from file in order
            UIHelper.createContextButtonList(c, binding.contextListLayout, readOutput, findNavController())
        }

    }
}