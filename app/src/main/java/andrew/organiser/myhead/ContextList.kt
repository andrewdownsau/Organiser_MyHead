package andrew.organiser.myhead

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import andrew.organiser.myhead.databinding.ContextListBinding

/**
 * Context List for organiser, navigates by default and redirects if context check result is valid
 */
class ContextList : Fragment() {

    private var _binding: ContextListBinding? = null
    private val fileName = "context_list.txt"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContextListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("--- Context List: onViewCreated ---")
        super.onViewCreated(view, savedInstanceState)
        val fragmentContext = requireContext().applicationContext

        //Navigate to second fragment using plus button
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_ContextList_to_ContextAddEdit)
        }

        //Check context file, create list if items exist
        val warning = "No contexts listed.\nPlease add context."
        val readOutput = FileHelper.checkFile(fragmentContext, fileName, warning, 2)
        if(readOutput == warning) {
            binding.subtitleText.visibility = View.VISIBLE
            binding.subtitleText.text = readOutput
        }
        else {
            binding.subtitleText.visibility = View.GONE
            //Create list of context buttons from file in order
            //val sortedRead = FileHelper.sortTaskList(readOutput)
            UIHelper.createContextButtonList(fragmentContext, binding.contextListLayout, readOutput, findNavController())
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}