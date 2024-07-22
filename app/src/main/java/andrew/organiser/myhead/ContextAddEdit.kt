package andrew.organiser.myhead

import andrew.organiser.myhead.databinding.ContextAddEditBinding
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
    private val fileName = "context_list.txt"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContextAddEditBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("---Context Add/Edit ---")
        val fragmentContext = requireContext().applicationContext

        //Close button redirect
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        //Setting name by lookup if in edit mode
        val itemId = arguments?.getInt("itemId")

        // --- Edit Mode ---
        if(itemId != null && itemId > 0){
            val oldLineContent = FileHelper.getItemById(fragmentContext, fileName, itemId)
            if (oldLineContent != null) {
                binding.editContextName.setText(oldLineContent.drop(oldLineContent.indexOf(";") + 1))

                //Save button file write and redirect
                binding.btnSave.setOnClickListener {
                    val newLineContent = "$itemId;${binding.editContextName.text}"
                    if(newLineContent != oldLineContent){
                        //If old context has tasks, edit file name for context task list
                        FileHelper.transferTasks(fragmentContext, oldLineContent, newLineContent)

                        //Save edited entry to context list
                        FileHelper.editEntry(fragmentContext, fileName, itemId, newLineContent, 2 )
                    }

                    findNavController().popBackStack()
                }

                //Delete button deletes entry
                binding.btnDelete.setOnClickListener {
                    FileHelper.deleteEntry(fragmentContext, fileName, itemId)
                    findNavController().popBackStack()
                }
            }
        }

        //--- Create New Mode ---
        else{
            //Save button file write and redirect
            binding.btnSave.setOnClickListener {
                val newLineContent = "${binding.editContextName.text}"
                FileHelper.createNewEntry(fragmentContext, fileName, newLineContent)

                findNavController().popBackStack()
            }

            //Delete button is disabled and has no onClick function
            binding.btnDelete.setBackgroundResource(R.drawable.shadow_button_disabled)
            binding.btnDelete.setTextColor(Color.parseColor("#555555"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}