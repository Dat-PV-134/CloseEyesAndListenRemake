package com.rekoj134.closeeyesandlisten.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.storage.storage
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.epoxy.ViewBindingKotlinModel
import com.rekoj134.closeeyesandlisten.model.MusicItem
import com.rekoj134.closeeyesandlisten.model.PlayList
import com.rekoj134.closeeyesandlisten.model.User
import com.rekoj134.closeeyesandlisten.remake.R
import com.rekoj134.closeeyesandlisten.remake.databinding.DialogChangeNameBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.DialogChangePassBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.DialogCreateNewPlaylistBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.FragmentProfileBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.ItemPlaylistBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.ItemPlaylistInProfileBinding
import com.rekoj134.closeeyesandlisten.ui.activity.LoginAndSignUpActivity
import com.rekoj134.closeeyesandlisten.ui.activity.MainActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val storage by lazy { Firebase.storage }
    private lateinit var myUser: User

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireActivity() as MainActivity).handleBackpress()
            }
        })

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(requireContext(), LoginAndSignUpActivity::class.java).apply {
                this.action = Constants.ACTION_SIGN_UP
            })
        }

        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(requireContext(), LoginAndSignUpActivity::class.java).apply {
                this.action = Constants.ACTION_LOG_IN
            })
        }
        
        binding.btnSignOut.setOnClickListener {
            Firebase.auth.signOut()
            updateViewSignedOut()
        }

        binding.imgProfile.setOnClickListener {
            val intent = Intent()
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            pickImageLauncher.launch(intent)
        }

        binding.tvChangePass.setOnClickListener {
            showDialogChangePass()
        }

        binding.tvChangeInfo.setOnClickListener {
            showDialogChangeName()
        }

        binding.vAddNewPlayList.setOnClickListener {
            showDialogCreateNewPlaylist()
        }
    }

    private fun showDialogCreateNewPlaylist() {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DialogCreateNewPlaylistBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)
        val dialog = builder.create()

        binding.btnCreateNew.setOnClickListener {
            val database = Firebase.database.reference
            val tempPlayList = ArrayList<PlayList>()
            myUser.playlist?.let {
                tempPlayList.addAll(it)
            }
            tempPlayList.add(PlayList(0, binding.etReNewName.text.toString(), "", ArrayList<MusicItem>()))
            database.child("users").child(auth.currentUser?.uid!!).child(Constants.CHILD_PLAYLIST).setValue(tempPlayList)
            Toast.makeText(requireContext(), "Tạo playlist thành công", Toast.LENGTH_SHORT).show()
            setupView()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showDialogChangeName() {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DialogChangeNameBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)
        val dialog = builder.create()

        binding.apply {
            btnChangeName.setOnClickListener {
                val database = Firebase.database.reference
                database.child("users").child(auth.currentUser?.uid!!).child(Constants.CHILD_NAME).setValue(binding.etReNewName.text.toString())
                Toast.makeText(requireContext(), "Thay đổi tên thành công", Toast.LENGTH_SHORT).show()
                setupView()
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showDialogChangePass() {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DialogChangePassBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)
        val dialog = builder.create()

        binding.apply {
            binding.btnChangePass.setOnClickListener {
                if (binding.etNewPass.text.toString().isEmpty() || binding.etNewPass.text.toString().length < 6) {
                    binding.etNewPass.error = "Mật khẩu không hợp lệ (Từ 6 ký tự trở lên)"
                } else if (binding.etNewPass.text.toString() != binding.etReNewPass.text.toString()) {
                    binding.etReNewPass.error = "Mật khẩu không khớp"
                } else if (binding.etOldPass.text.toString() != myUser.password) {
                    binding.etOldPass.error = "Mật khẩu cũ không đúng"
                } else {
                    changePass(binding.etReNewPass.text.toString())
                    dialog.dismiss()
                }
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun changePass(password: String) {
        val user = Firebase.auth.currentUser

        user?.updatePassword(password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val database = Firebase.database.reference
                    database.child("users").child(auth.currentUser?.uid!!).child(Constants.CHILD_PASSWORD).setValue(password)
                    Toast.makeText(requireContext(), "Thay đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveProfileImage(uri: Uri) {
        val link = "${auth.currentUser?.uid}.jpg"
        val ref = storage.reference.child("profile_images/$link")
        val uploadTask = ref.putFile(uri)

        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                Toast.makeText(requireContext(), "Cập nhật ảnh thất bại", Toast.LENGTH_SHORT).show()
                task.exception?.let {
                    throw it
                }
            }
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Toast.makeText(requireContext(), "Cập nhật ảnh thành công", Toast.LENGTH_SHORT).show()
                val database = Firebase.database.reference
                database.child("users").child(auth.currentUser?.uid!!).child(Constants.CHILD_PROFILE_IMAGE).setValue(downloadUri.toString())
                Glide.with(requireContext()).load(downloadUri).error(R.drawable.ic_launcher_foreground).into(binding.imgProfile)
            } else {
                //
            }
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                selectedImageUri?.let {
                    saveProfileImage(it)
                }
            }
        }

    private fun initData() {
        auth = Firebase.auth
    }

    private fun setupView() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            updateViewSignedOut()
        } else {
            updateViewSignedIn(currentUser)
        }
    }

    private fun updateViewSignedIn(user: FirebaseUser) {
        binding.btnSignUp.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE
        binding.tvChangeInfo.visibility = View.VISIBLE
        binding.tvChangePass.visibility = View.VISIBLE
        binding.btnSignOut.visibility = View.VISIBLE
        binding.tvHelloUser.visibility = View.VISIBLE

        val database = Firebase.database
        val myRef = database.reference.child(Constants.CHILD_USERS)
        val query = myRef.orderByChild("id").equalTo(user.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val mapResult = snapshot.getValue<HashMap<String, User>>()
                mapResult?.let {
                    myUser = it[user.uid]!!
                    binding.tvHelloUser.text = "Xin chào, ${it[user.uid]?.name.toString()}"
                    Glide.with(requireContext()).load(it[user.uid]?.image).error(R.drawable.ic_launcher_foreground).into(binding.imgProfile)
                    updateListPlayList()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun updateListPlayList() {
        myUser.playlist?.let {
            binding.rvPlayList.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            var count = 0
            binding.rvPlayList.withModels {
                it.forEach { item ->
                    PlaylistProfileItemModel(requireContext(), item, {
                        openPlayList()
                    }, {
                        deletePlayList(it)
                    }).id(count).addTo(this)
                    count++
                }
            }
            Log.e("COUNTANCUTKO", count.toString())
        }
    }

    private fun deletePlayList(playList: PlayList) {
        val database = Firebase.database.reference
        val tempPlayList = ArrayList<PlayList>()
        myUser.playlist?.let {
            tempPlayList.addAll(it)
        }
        tempPlayList.remove(playList)
        database.child("users").child(auth.currentUser?.uid!!).child(Constants.CHILD_PLAYLIST).setValue(tempPlayList)
        Toast.makeText(requireContext(), "Xóa playlist thành công", Toast.LENGTH_SHORT).show()
        setupView()
    }

    private fun openPlayList() {

    }

    data class PlaylistProfileItemModel(
        val context: Context,
        val playlistItem: PlayList,
        val onClickItem:(PlayList) -> Unit,
        val onClickDelete:(PlayList) -> Unit
    ) : ViewBindingKotlinModel<ItemPlaylistInProfileBinding>(R.layout.item_playlist_in_profile) {
        override fun ItemPlaylistInProfileBinding.bind() {
            Glide.with(context).load(playlistItem.previewImage).error(R.drawable.img_playlist_none).into(imgPreviewPlaylist)
            tvPlaylistName.text = playlistItem.playListName
            root.setOnClickListener {
                onClickItem(playlistItem)
            }

            imgDelete.setOnClickListener {
                onClickDelete(playlistItem)
            }
        }
    }

    private fun updateViewSignedOut() {
        binding.btnSignUp.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.VISIBLE
        binding.tvChangeInfo.visibility = View.GONE
        binding.tvChangePass.visibility = View.GONE
        binding.btnSignOut.visibility = View.GONE
        binding.tvHelloUser.visibility = View.GONE
        binding.imgProfile.setImageResource(R.drawable.ic_launcher_foreground)
    }

    override fun onResume() {
        setupView()
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ProfileFragment {
            val args = Bundle()
            val fragment: ProfileFragment = ProfileFragment()
            fragment.arguments = args
            return fragment
        }
    }
}