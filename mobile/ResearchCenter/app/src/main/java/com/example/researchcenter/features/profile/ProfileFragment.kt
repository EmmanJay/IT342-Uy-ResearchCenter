package com.example.researchcenter.features.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.researchcenter.R
import com.example.researchcenter.features.auth.LoginActivity
import com.example.researchcenter.features.main.MainActivity
import com.example.researchcenter.shared.api.AuthApi
import com.example.researchcenter.shared.api.RetrofitClient
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.*
import com.example.researchcenter.shared.ui.UserAvatarView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.researchcenter.shared.data.AppDatabase

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var tvStatMemberSince: TextView
    private lateinit var tvStatRepos: TextView
    private lateinit var avatarProfile: UserAvatarView
    private lateinit var ivCameraEdit: ImageView

    private var selectedImageUri: Uri? = null

    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        val ctx = context ?: return null
        return try {
            val inputStream = ctx.contentResolver.openInputStream(uri) ?: return null
            val outputFile = java.io.File(ctx.filesDir, "profile_avatar.jpg")
            val outputStream = java.io.FileOutputStream(outputFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val ctx = context ?: return@let
                val persistentUri = copyUriToInternalStorage(uri)
                if (persistentUri != null) {
                    selectedImageUri = persistentUri
                    SessionManager.saveLocalAvatarUri(ctx, persistentUri.toString())
                    try {
                        avatarProfile.setImageURI(persistentUri)
                        Toast.makeText(ctx, "Profile photo updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Failed to set image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(ctx, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tv_profile_name)
        tvEmail = view.findViewById(R.id.tv_profile_email)
        tvRoleBadge = view.findViewById(R.id.tv_role_badge)
        tvStatMemberSince = view.findViewById(R.id.tv_stat_member_since)
        tvStatRepos = view.findViewById(R.id.tv_stat_repos)
        avatarProfile = view.findViewById(R.id.avatar_profile)
        ivCameraEdit = view.findViewById(R.id.iv_camera_edit)

        val btnEdit = view.findViewById<MaterialButton>(R.id.btn_edit_profile)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)

        // Set cached values first
        val sessionName = SessionManager.getName(requireContext()) ?: "User"
        val sessionEmail = SessionManager.getEmail(requireContext()) ?: ""
        tvName.text = sessionName
        tvEmail.text = sessionEmail
        
        val cachedLocalUri = SessionManager.getLocalAvatarUri(requireContext())
        val cachedBackendPic = SessionManager.getProfilePicture(requireContext())
        if (!cachedBackendPic.isNullOrEmpty()) {
            avatarProfile.setUser(sessionName, sessionEmail, cachedBackendPic)
        } else if (cachedLocalUri != null) {
            avatarProfile.setImageURI(Uri.parse(cachedLocalUri))
        } else {
            avatarProfile.setUser(sessionName, sessionEmail)
        }
        
        tvRoleBadge.text = SessionManager.getRole(requireContext()) ?: "USER"

        // Camera icon click to pick profile image
        ivCameraEdit.setOnClickListener { openImagePicker() }
        avatarProfile.setOnClickListener { openImagePicker() }

        btnEdit.setOnClickListener { showEditProfileDialog() }
        btnLogout.setOnClickListener {
            val ctx = requireContext()
            SessionManager.clearSession(ctx)
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        loadMe()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun loadMe() {
        RetrofitClient.createService<AuthApi>().getMe().enqueue(object : Callback<ApiResponse<UserData>> {
            override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                val wrapper = response.body()
                if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                    activity?.runOnUiThread {
                        val user = wrapper.data
                        val ctx = context ?: return@runOnUiThread
                        SessionManager.saveUserId(ctx, user.id)
                        SessionManager.saveEmail(ctx, user.email)
                        val fullName = "${user.firstname} ${user.lastname}".trim()
                        SessionManager.saveName(ctx, fullName)
                        SessionManager.saveRole(ctx, user.role)

                        tvName.text = fullName
                        tvEmail.text = user.email
                        tvRoleBadge.text = user.role
                        val cachedLocalUri = SessionManager.getLocalAvatarUri(ctx)
                        val backendPic = user.profilePicture
                        SessionManager.saveProfilePicture(ctx, backendPic)
                        if (selectedImageUri == null) {
                            if (!backendPic.isNullOrEmpty()) {
                                avatarProfile.setUser(fullName, user.email, backendPic)
                            } else if (cachedLocalUri != null) {
                                avatarProfile.setImageURI(Uri.parse(cachedLocalUri))
                            } else {
                                avatarProfile.setUser(fullName, user.email)
                            }
                        }

                        // Stats
                        user.createdAt?.let { dateStr ->
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                val date = parser.parse(dateStr.take(19))
                                if (date != null) {
                                    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                    tvStatMemberSince.text = formatter.format(date)
                                }
                            } catch (_: Exception) {
                                tvStatMemberSince.text = dateStr.take(7)
                            }
                        }

                        // Repos Count from local DB cache
                        val ctxLocal = context
                        if (ctxLocal != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val db = AppDatabase.getInstance(ctxLocal)
                                    val count = db.repositoryDao().getAll().size
                                    withContext(Dispatchers.Main) {
                                        tvStatRepos.text = count.toString()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        (activity as? MainActivity)?.refreshAvatar()
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {}
        })
    }

    private fun showEditProfileDialog() {
        val parts = (SessionManager.getName(requireContext()) ?: " ").split(" ", limit = 2)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null)
        val etFirst = view.findViewById<TextInputEditText>(R.id.et_first_name)
        val etLast = view.findViewById<TextInputEditText>(R.id.et_last_name)
        etFirst.setText(parts.getOrElse(0) { "" })
        etLast.setText(parts.getOrElse(1) { "" })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_profile)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val first = etFirst.text.toString().trim()
                val last = etLast.text.toString().trim()
                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(context, "Name fields cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveProfile(first, last)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveProfile(firstname: String, lastname: String) {
        RetrofitClient.createService<AuthApi>()
            .updateProfile(UpdateProfileRequest(firstname, lastname))
            .enqueue(object : Callback<ApiResponse<UserData>> {
                override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                    val wrapper = response.body()
                    if (response.isSuccessful && wrapper?.success == true && wrapper.data != null) {
                        activity?.runOnUiThread {
                            val user = wrapper.data
                            val fullName = "${user.firstname} ${user.lastname}".trim()
                            val ctx = context ?: return@runOnUiThread
                            SessionManager.saveName(ctx, fullName)
                            tvName.text = fullName
                            if (selectedImageUri == null) {
                                avatarProfile.setUser(fullName, user.email)
                            }
                            Toast.makeText(ctx, "Profile updated", Toast.LENGTH_SHORT).show()
                            (activity as? MainActivity)?.refreshAvatar()
                        }
                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Could not update profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<UserData>>, t: Throwable) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (::tvName.isInitialized) {
            loadMe()
        }
    }
}
