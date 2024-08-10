package andrew.organiser.myhead_v2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import andrew.organiser.myhead_v2.databinding.ActivityMainBinding
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        //Navigate to settings fragment
        binding.fabSettings.setOnClickListener {
            navController.navigate(R.id.navigate_to_Settings)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        //Public global variables
        const val CONTEXT_FILE = "context_list.txt"
        const val TASK_FILE = "task_list.txt"
        const val ARCHIVE_FILE = "archive_list.txt"
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val SIMPLE_DF =  SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        const val DEFAULT_DATE = "01/01/2000"


    }
}