package andrew.organiser.myhead_v2

import andrew.organiser.myhead_v2.databinding.ContextAddEditBinding
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


class ContextAddEdit : Fragment() {

    private var _binding: ContextAddEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContextAddEditBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("---Context Add/Edit ---")
        val fragmentContext = requireContext().applicationContext

        //Check if any tasks should be archived based on time
        FileHelper.archiveTasks(fragmentContext)

        //Close button redirect
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        //Setting name by lookup if in edit mode
        val contextObject = arguments?.getString("contextObject")

        //--- Create New Mode ---
        if(contextObject == null){
            //Save button file write and redirect
            binding.btnSave.setOnClickListener {
                var newContext = "Name:${binding.editContextName.text}\t"
                val validWarning = FileHelper.validateEntry(fragmentContext, "", newContext, MainActivity.CONTEXT_FILE)
                if(validWarning == "valid"){
                    newContext += "\n"
                    FileHelper.appendFile(fragmentContext, MainActivity.CONTEXT_FILE, newContext)
                    findNavController().popBackStack()
                }
                else {
                    binding.labelWarning.visibility = View.VISIBLE
                    binding.labelWarning.text = validWarning
                }
            }

            //Delete button is disabled and has no onClick function
            binding.btnDelete.setBackgroundResource(R.drawable.shadow_button_disabled)
            binding.btnDelete.setTextColor(Color.parseColor("#555555"))
        }
        // --- Edit Mode ---
        else{
            val contextName = FileHelper.getObjectValue(contextObject, "Name:")
            if(contextName != null){
                val originalContext = "Name:${contextName}\t"
                binding.editContextName.setText(contextName)

                //Save button file write and redirect
                binding.btnSave.setOnClickListener {
                    val newContextName = binding.editContextName.text.toString()
                    val newContext = "Name:${newContextName}\t"
                    val validWarning = FileHelper.validateEntry(fragmentContext, originalContext, newContext, MainActivity.CONTEXT_FILE)
                    if(validWarning == "valid"){
                        //If old context has tasks, edit file name for context task list
                        FileHelper.transferTasks(fragmentContext, contextName, newContextName)

                        //Save edited entry to context list
                        FileHelper.editEntry(fragmentContext, MainActivity.CONTEXT_FILE, contextObject, newContext )
                        findNavController().popBackStack()
                    }
                    else {
                        binding.labelWarning.visibility = View.VISIBLE
                        binding.labelWarning.text = validWarning
                    }
                }

                //Delete button deletes entry and tasks attached to context
                binding.btnDelete.setOnClickListener {
                    FileHelper.deleteEntry(fragmentContext, MainActivity.CONTEXT_FILE, contextObject)
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}