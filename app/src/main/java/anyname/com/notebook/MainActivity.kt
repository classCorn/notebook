package anyname.com.notebook

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import anyname.com.notebook.controllers.CRealmControllers
import io.realm.Realm
import io.realm.RealmConfiguration
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView


class MainActivity : AppCompatActivity() {

    private lateinit var btnAdd: ImageButton
    private lateinit var layEdit: RelativeLayout
    private lateinit var txtEdit: EditText
    private lateinit var scrollView: ScrollView
    private var editIndex = -1
    private var isHideControl = true

    override fun onBackPressed() {

        if (layEdit.visibility == View.VISIBLE) {
            showEditLayout(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRealm()
        initUI()
        initDynamical()
    }

    private fun initRealm() {
        Realm.init(baseContext)

        val config = RealmConfiguration.Builder()
                .name("db.realm")
                .schemaVersion(1L)
                .migration(CRealmControllers.RealmMigrations())
                .build()
        Realm.setDefaultConfiguration(config)
    }

    private fun initUI() {

        txtEdit = findViewById(R.id.txt_edit)

        btnAdd = findViewById(R.id.btn_main_add)
        btnAdd.setOnClickListener {
            txtEdit.setText("")
            showEditLayout(true)
        }

        val btnWrite = findViewById<Button>(R.id.btn_edit_write)
        btnWrite.setOnClickListener {
            val inputText = txtEdit.text.toString()
            if (editIndex > -1) {
                listView.forEach { view ->
                    if (view.tag as Int == editIndex) {
                        val tv = view.findViewById<TextView>(R.id.item_text)
                        tv.text = inputText
                        list[editIndex] = inputText
                    }
                }
                updateRealm()
            } else {
                addItem(inputText)
            }
            updateRealm()
            showEditLayout(false)
        }


        layEdit = findViewById(R.id.lay_edit)
        layEdit.visibility = View.GONE
        layEdit.setOnClickListener {
            showEditLayout(false)
        }

        scrollView = findViewById(R.id.lay_scroll)
        scrollView.requestDisallowInterceptTouchEvent(true)
        scrollView.setOnTouchListener { _, _ ->
            isHideControl = true
            return@setOnTouchListener false
        }

        var oldY = 0
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (isHideControl) {
                val y = scrollView.scrollY
                val dy = oldY - y
                oldY = y
                if (dy * dy > 25) {
                    showControls(dy > 0)
                }
            }
        }
    }

    private fun updateRealm() {
        CRealmControllers.setItems(list)
    }

    private fun showControls(show: Boolean) {
        val layControls = findViewById<LinearLayout>(R.id.lay_main_controls)
        val step = if (show) 0f else 200f
        layControls.animate().translationY(step).setInterpolator(LinearInterpolator()).start()
    }

    private fun showEditLayout(show: Boolean, index: Int = -1) {

        layEdit.visibility = if (show) View.VISIBLE else View.GONE
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 500
        anim.addUpdateListener {
            val v = it.animatedValue as Float
            layEdit.alpha = v
        }
        anim.start()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            txtEdit.requestFocus()
            imm.showSoftInput(txtEdit, InputMethodManager.SHOW_IMPLICIT)
        } else {
            val view = layEdit
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        editIndex = index
        if (index >= 0) {
            txtEdit.setText(list[index])
            txtEdit.setSelection(txtEdit.text.length)
        }
    }

    private var list = mutableListOf<String>()
    private var listView = mutableListOf<View>()
    private fun initDynamical() {
        list = CRealmControllers.getItems()
        listView = mutableListOf()

        list.forEach {
            addItem(it, false)
        }
    }

    private fun addItem(text: String, addToList: Boolean = true) {

        val parent = findViewById<LinearLayout>(R.id.lay_dynamical)
        val inflater = baseContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val lay = inflater.inflate(R.layout.item_of_list, null)
        lay.tag = listView.size
        lay.setOnClickListener {
            showEditLayout(true, it.tag as Int)
        }
        val remove = lay.findViewById<ImageButton>(R.id.item_remove)
        remove.tag = listView.size

        lay.findViewById<TextView>(R.id.item_text).text = text

        setGesture(lay)
        listView.add(lay)
        parent.addView(lay)

        isHideControl = false
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }

        lay.scaleY = 0f
        lay.animate().scaleY(1f).setInterpolator(AccelerateInterpolator()).setDuration(300).start()

        remove.setOnClickListener {
            val tag = it.tag as Int

            val listenerAnimator = object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    parent.removeView(listView[tag])
                    listView.removeAt(tag)
                    list.removeAt(tag)
                    listView.forEachIndexed { index, view ->
                        view.tag = index
                        view.findViewById<ImageButton>(R.id.item_remove).tag = index
                        val tv = view.findViewById<TextView>(R.id.item_text)
                        val t = tv.text.toString()
                        tv.text = t
                    }
                    updateRealm()
                }

                override fun onAnimationCancel(p0: Animator?) {}

                override fun onAnimationStart(p0: Animator?) {}

            }
            if (tag < listView.size) {
                listView[tag].pivotX = listView[tag].width - 29f
                listView[tag].pivotY = 29f
                listView[tag].animate().translationX(2000f).alpha(0.5f).setDuration(300).setListener(listenerAnimator).start()
            }
        }

        if (addToList) {
            list.add(text)
            updateRealm()
        }
    }

    private fun setGesture(view: View) {
//        val gesture = GestureListener(view)
//        gesture.setOnX {
//            view.translationX = it
//        }
    }

}
