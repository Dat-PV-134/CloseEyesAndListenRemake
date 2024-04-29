package com.rekoj134.closeeyesandlisten.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.rekoj134.closeeyesandlisten.model.User
import com.rekoj134.closeeyesandlisten.remake.databinding.FragmentSignUpBinding
import com.rekoj134.closeeyesandlisten.ui.activity.LoginAndSignUpActivity

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupView()
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })

        binding.btnSignUp.setOnClickListener {
            signUp()
        }

        binding.btnLogin.setOnClickListener {
            (requireActivity() as LoginAndSignUpActivity).goToLogin()
        }
    }

    private fun initData() {

    }

    private fun setupView() {

    }

    private fun signUp() {
        val name = binding.etSignUpName.text.toString().trim()
        val email = binding.etSignUpUserName.text.toString().trim()
        val password = binding.etSignUpPassWord.text.toString().trim()
        val rePassword = binding.etRePassWord.text.toString().trim()

        if (name.isEmpty()) {
            binding.etSignUpName.error = "Họ tên không được bỏ trống"
        } else if (!email.matches(emailPattern.toRegex())) {
            binding.etSignUpUserName.error = "Email không hợp lệ"
        } else if (password.isEmpty() || password.length < 6) {
            binding.etSignUpPassWord.error = "Mật khẩu không hợp lệ (Từ 6 ký tự trở lên)"
        } else if (password != rePassword) {
            binding.etRePassWord.error = "Mật khẩu không khớp"
        } else {
            val auth = Firebase.auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Log.d("ANCUTKO", "createUserWithEmail:success")
                        Toast.makeText(
                            requireContext(),
                            "Đăng ký tài khoản thành công",
                            Toast.LENGTH_SHORT,
                        ).show()
                        val user = auth.currentUser
                        writeNewUser(user!!.uid, email, password, name)
                        requireActivity().finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("ANCUTKO", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            requireContext(),
                            "Tài khoản đã tồn tại",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }

    private fun writeNewUser(userId: String, username: String, password: String, name: String) {
        val user = User(userId, username, password, name)
        val database = Firebase.database.reference
        database.child("users").child(userId).setValue(user)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SignUpFragment {
            val args = Bundle()
            val fragment: SignUpFragment = SignUpFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
