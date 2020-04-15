package science.credo.mobiledetector.statistics

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.activity_settings.toolbar
import kotlinx.android.synthetic.main.activity_settings.viewProgress
import kotlinx.android.synthetic.main.activity_statistics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import science.credo.mobiledetector.R
import science.credo.mobiledetector.detector.Hit
import science.credo.mobiledetector.utils.UiUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException


class StatisticsActivity : AppCompatActivity(), HitsAdapter.OnClickListener {

    lateinit var detections: Array<Hit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_toolbar_back)


        GlobalScope.launch(Dispatchers.Main) {
            viewProgress.visibility = View.VISIBLE
            detections = load()
            println("====== ${detections.size}")
            rvDetections.layoutManager = GridLayoutManager(this@StatisticsActivity, 3)
            rvDetections.adapter = HitsAdapter(
                this@StatisticsActivity,
                detections,
                this@StatisticsActivity,
                (UiUtils.getScreenWidth() - UiUtils.dpToPx(20)) / 3
            )
            viewProgress.visibility = View.GONE
        }

    }


    private suspend fun load(): Array<Hit> {
        return GlobalScope.async {
            val directory =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/detections"
            val root = File(directory)
            val gson = Gson()
            val detections = ArrayList<Hit>()
            for (detectionFolder in (root.listFiles() ?: emptyArray()).sortedDescending()) {
                for (detectionFile in detectionFolder.listFiles() ?: emptyArray()) {
                    val br = BufferedReader(FileReader(detectionFile))
                    try {
                        val sb = StringBuilder()
                        do {
                            val line = br.readLine();
                            sb.append(line ?: "")
                        } while (line != null)
                        detections.add(
                            gson.fromJson<Hit>(
                                sb.toString().replace("\n", ""),
                                Hit::class.java
                            )
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        br.close()
                    }
                }
            }
            return@async detections.toTypedArray()

        }.await()
    }

    override fun onClick(hit: Hit) {
        HitInfoDialog.newInstance(hit).show(supportFragmentManager,"${hit.timestamp}")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

}