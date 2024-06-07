package com.example.unisystems.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.unisystems.R
import com.example.unisystems.activity.MenuActivity
import com.example.unisystems.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StudentFragment : Fragment() {
    private lateinit var surname: EditText
    private lateinit var name: EditText
    private lateinit var patronym: EditText
    private lateinit var serialNumber: EditText
    private lateinit var groupName: EditText
    private lateinit var facultySpinner: Spinner
    private lateinit var eduFormSpinner: Spinner
    private lateinit var createBtn: Button

    private lateinit var issueDateBtn: Button
    private lateinit var expDateBtn: Button

    private lateinit var issueDate: TextView
    private lateinit var expDate: TextView

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser

    private val databaseUrl: String =
        "https://uni-k-systems-default-rtdb.europe-west1.firebasedatabase.app"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.student_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surname = view.findViewById(R.id.secondName)
        name = view.findViewById(R.id.firstName)
        patronym = view.findViewById(R.id.patronym)
        serialNumber = view.findViewById(R.id.input_serial_number)
        createBtn = view.findViewById(R.id.create_account_btn)
        groupName = view.findViewById(R.id.input_group)
        facultySpinner = view.findViewById(R.id.facultySpinner)
        eduFormSpinner = view.findViewById(R.id.educationFormSpinner)

        issueDateBtn = view.findViewById(R.id.issue_date)
        expDateBtn = view.findViewById(R.id.expiration_date)

        issueDate = view.findViewById(R.id.date1)
        expDate = view.findViewById(R.id.date2)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        firebaseDatabase = FirebaseDatabase.getInstance()

        val calendar = Calendar.getInstance()

        setupDatePickerButton(calendar, issueDateBtn, issueDate)
        setupDatePickerButton(calendar, expDateBtn, expDate)

        createBtn.setOnClickListener {
            createAccount()
        }

        val facultyOptions = arrayOf("Інженерно-хімічний факультет",
                "Приладобудівний факультет",
                "Радіотехнічний факультет",
                "Фізико-математичний факультет",
                "Факультет інформатики та обчислювальної техніки",
                "Факультет біомедичної інженерії",
                "Факультет біотехнології і біотехніки",
                "Факультет електроенерготехніки та автоматики",
                "Факультет електроніки",
                "Факультет лінгвістики",
                "Факультет менеджменту та маркетингу",
                "Факультет прикладної математики",
                "Факультет соціології і права",
                "Хіміко-технологічний факультет",
                "Інститут аерокосмічних технологій",
                "Інститут атомної та теплової енергетики",
                "Інститут енергозбереження та енергоменеджменту",
                "Інститут матеріалознавства та зварювання ім. Є.О. Патона",
                "Інститут прикладного системного аналiзу",
                "Інститут спеціального зв'язку та захисту інформації",
                "Інститут телекомунікаційних систем",
                "Видавничо-поліграфічний інститут",
                "Механіко-машинобудівний інститут",
                "Фізико-технічний інститут")


        val educationFormOptions = arrayOf("Денна", "Вечірня", "Заочна", "Дистанційна", "Мережева", "Дуальна")

        val facultyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, facultyOptions)
        val eduFormAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, educationFormOptions)

        facultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        eduFormAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        facultySpinner.adapter = facultyAdapter
        eduFormSpinner.adapter = eduFormAdapter
    }

    private fun createAccount() {
        val database = Firebase.database(databaseUrl)
        val myRef = database.getReference("users")

        val uid = user.uid
        val photoUrl = user.photoUrl.toString()
        val userTag = "student"
        val surnameText = surname.text.toString().trim()
        val nameText = name.text.toString().trim()
        val patronymText = patronym.text.toString().trim()
        val serialNumText = serialNumber.text.toString().trim()
        val issueDateText = issueDate.text.toString().trim()
        val expDateText = expDate.text.toString().trim()
        val faculty = facultySpinner.selectedItem.toString().trim()
        val educationForm = eduFormSpinner.selectedItem.toString().trim()
        val groupNameText = groupName.text.toString().trim()

        val groupNamePattern = "^[А-ЯІ]{2}-\\d{2}$"
        val serialNumberPattern = "^[А-ЯІ]{2}\\d{8}$"

        if (surnameText.isEmpty() || nameText.isEmpty() || patronymText.isEmpty() || serialNumText.isEmpty() ||
            issueDateText.isEmpty() || expDateText.isEmpty() || faculty.isEmpty() || educationForm.isEmpty() ||
            groupNameText.isEmpty()) {

            Log.e("StudentFragment", "Не всі поля заповнені")
            Toast.makeText(requireContext(), "Будь ласка, заповніть всі поля", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (issueDateText == expDateText) {
            Log.e(TAG, "Дата видачі і дата закінчення є однаковими")
            Toast.makeText(
                requireContext(),
                "Дата видачі і дата закінчення не можуть бути однаковими",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!groupNameText.matches(Regex(groupNamePattern))) {
            Log.e(TAG, "Неправильний формат назви групи")
            Toast.makeText(
                requireContext(),
                "Неправильний формат назви групи. Використовуйте шаблон 'АА-01'",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!serialNumText.matches(Regex(serialNumberPattern))) {
            Log.e(TAG, "Невірний формат номеру студентського квитка")
            Toast.makeText(requireContext(), "Невірний формат номеру студентського квитка. Використовуйте шаблон 'АА12345678'", Toast.LENGTH_SHORT).show()
            return
        } else {
            if(surnameText.isNotEmpty() || nameText.isNotEmpty() || patronymText.isNotEmpty() || serialNumText.isNotEmpty() ||
                issueDateText.isNotEmpty() || expDateText.isNotEmpty() || faculty.isNotEmpty() || educationForm.isNotEmpty() ||
                groupNameText.isNotEmpty()) {
                val user = User(uid, photoUrl, userTag, surnameText, nameText, patronymText, serialNumText,
                    issueDateText, expDateText, faculty, educationForm, groupNameText)

                val userId = auth.currentUser!!.uid
                myRef.child(userId).setValue(user)

                updateUI()
            } else {
                Toast.makeText(requireContext(), "Будь ласка, заповніть всі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        val intent = Intent(requireContext(), MenuActivity::class.java)
        startActivity(intent)
    }

    private fun setupDatePickerButton(calendar: Calendar, button: Button, dateTextView: TextView) {
        button.setOnClickListener {
            val datePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateLabel(calendar, dateTextView)
            }
            DatePickerDialog(
                requireContext(),
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(
                    Calendar.DAY_OF_MONTH
                )
            ).show()
        }
    }

    private fun updateLabel(calendar: Calendar, date: TextView) {
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.UK)
        date.text = sdf.format(calendar.time)
        date.visibility = View.VISIBLE
    }

    companion object {
        const val TAG = "StudentFragment"
    }
}