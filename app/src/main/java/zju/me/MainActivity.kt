package zju.me

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE : Int = 1000
    private var allPermissionsGranted = false
    private lateinit var addressEdit : EditText
    private lateinit var streamEdit: EditText
    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 检查并申请权限
        var permissions = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        checkPermissions(permissions)
        addressEdit = findViewById(R.id.address)
        streamEdit = findViewById(R.id.stream)
        // 读取配置
        sp = getSharedPreferences("CrackDetection", MODE_PRIVATE)
        addressEdit.setText(sp.getString("address", ""))
        streamEdit.setText(sp.getString("stream", ""))
        // 添加按钮点击事件
        findViewById<Button>(R.id.connect).setOnClickListener {
            if (!allPermissionsGranted) {
                Toast.makeText(this,
                    "权限不足，请手动授权", Toast.LENGTH_LONG).show()
                startSettingActivity()
            } else {
                var url: String
                var streamPattern = "^/[a-zA-Z]{3,}/\\w{3,}$"
                var streamMatcher: Matcher = Pattern.compile(streamPattern).matcher(streamEdit.text)
                if (streamMatcher.find()) {
                    var address = addressEdit.text
                    var stream = streamEdit.text
                    url = "rtmp://$address$stream"
                    var urlPattern = "^(rtmp://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+$";
                    var urlMatcher: Matcher = Pattern.compile(urlPattern).matcher(url)
                    if (urlMatcher.find()) {
                        // 保存当前地址与应用名称 
                        var editor = sp.edit()
                        editor.putString("address", "$address")
                        editor.putString("stream", "$stream")
                        editor.commit()
                        intent = Intent(this, CameraActivity::class.java)
                        intent.putExtra("address", url)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "网络地址不合法", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "应用名称不合法", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var isAllGranted = true
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false
                    break
                }
            }
            allPermissionsGranted = isAllGranted
        }
    }

    private fun checkPermissions(permissions: Array<out String>) {
        var permissionGranted = true
        for (it in permissions) {
            if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                permissionGranted = false
                break
            }
        }
        if (!permissionGranted) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        } else {
            allPermissionsGranted = true
        }
    }

    private fun startSettingActivity() {
        var intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("package:$packageName")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
        startActivity(intent)
    }

    private fun getRandomAlphaString(length: Int): String? {
        val base = "abcdefghijklmnopqrstuvwxyz"
        val random = Random()
        val sb = StringBuilder()
        for (i in 0 until length) {
            val number: Int = random.nextInt(base.length)
            sb.append(base[number])
        }
        return sb.toString()
    }

    private fun getRandomAlphaDigitString(length: Int): String? {
        val base = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = Random()
        val sb = StringBuilder()
        for (i in 0 until length) {
            val number: Int = random.nextInt(base.length)
            sb.append(base[number])
        }
        return sb.toString()
    }
}