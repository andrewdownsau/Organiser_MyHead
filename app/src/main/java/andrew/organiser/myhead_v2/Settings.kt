package andrew.organiser.myhead_v2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import andrew.organiser.myhead_v2.databinding.SettingsBinding
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream

/**
 * Settings Fragment, contains backup option for storing text data into phone
 */
class Settings : Fragment() {

    private var _binding: SettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("--- Context List: onViewCreated ---")
        super.onViewCreated(view, savedInstanceState)
        val fragmentContext = requireContext().applicationContext

        //Enable download button to export data file
        binding.buttonDownloadExport.setOnClickListener{
            var fileReadOutput = "--- Context ---\n" + FileHelper.readFile(fragmentContext, MainActivity.CONTEXT_FILE)?.joinToString("\n")
            fileReadOutput += "--- Task List ---\n" + FileHelper.readFile(fragmentContext, MainActivity.TASK_FILE)?.joinToString("\n")
            fileReadOutput += "--- Archive List ---\n" + FileHelper.readFile(fragmentContext, MainActivity.ARCHIVE_FILE)?.joinToString("\n")

            writeToFile(fragmentContext, fileReadOutput)

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun writeToFile(c: Context, data: String) {

        try {
            val resolver = c.contentResolver
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "My_Head_Export_File.txt")
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text")
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)

            //val cr: ContentResolver = getContentResolver()
            val os: OutputStream? = uri?.let { resolver.openOutputStream(it,"wt") }

            if (os != null) {
                Toast.makeText(c,"Download Started...", Toast.LENGTH_SHORT).show()
                os.write(data.toByteArray())
                os.flush()
                os.close()
                Toast.makeText(c,"Download Completed !!!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}