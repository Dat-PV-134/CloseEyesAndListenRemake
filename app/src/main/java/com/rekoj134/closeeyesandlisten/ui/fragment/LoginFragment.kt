package com.rekoj134.closeeyesandlisten.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.rekoj134.closeeyesandlisten.remake.databinding.FragmentLoginBinding
import com.rekoj134.closeeyesandlisten.ui.activity.LoginAndSignUpActivity


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupView()
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        })

        binding.tvSignUp.setOnClickListener {
            (requireActivity() as LoginAndSignUpActivity).goToSignUp()
        }

        binding.btnSignIn.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val userEmail = binding.etUserName.text.toString()
        val userPassword = binding.etPassWord.text.toString()

        val auth = FirebaseAuth.getInstance()

        if (!userEmail.matches(emailPattern.toRegex())) {
            binding.etUserName.error = "Email không hợp lệ"
        } else if (userPassword.isEmpty() || userPassword.length < 6) {
            binding.etPassWord.error = "Tài khoản hoặc mật khẩu sai"
        } else {
            auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                        requireActivity().finish()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Tài khoản hoặc mật khẩu sai",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun initData() {

    }

    private fun setupView() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): LoginFragment {
            val args = Bundle()
            val fragment: LoginFragment = LoginFragment()
            fragment.arguments = args
            return fragment
        }
    }
}