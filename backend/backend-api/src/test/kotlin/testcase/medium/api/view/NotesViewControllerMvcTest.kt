/*
 * NotesViewControllerMvcTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.medium.api.view

import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.NoteId
import com.fj.omnimemo.core.note.model.NoteLanguage
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import com.fj.omnimemo.view.NotesViewController
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import test.com.fj.omnimemo.core.annotation.MediumTest
import test.com.fj.omnimemo.core.note.randomNote
import test.com.fj.omnimemo.core.user.randomUser

/**
 * Medium tests for [NotesViewController]: verifies the notes directory and single-note
 * pages render with correct HTML, breadcrumbs, and status codes — for both authenticated
 * and unauthenticated requests.
 *
 * Security filters are excluded ([addFilters] = false); the authentication state is
 * controlled directly via [SecurityContextHolder] to exercise all branches of
 * [com.fj.omnimemo.view.resolveCurrentUserId]:
 *
 * - No authentication (`authentication` == null)
 * - Authentication present but `isAuthenticated` == false
 * - Principal == `"anonymousUser"` sentinel
 * - Principal is a non-UUID string (parse failure → null requester)
 * - Principal is a valid UUID string (authenticated user)
 *
 * The authenticated note test also verifies the `canEdit` model attribute: the edit
 * button is shown only when the requester is the note's author.
 *
 * [NoteNotFoundException] triggers a 404 via [org.springframework.web.server.ResponseStatusException].
 * The [userProfileCache] bean is left unstubbed for all tests so that `currentUserEmail`
 * evaluates to null — this keeps the login-button branch of the header active and avoids
 * needing a CSRF token stub (the logout button and its `${_csrf.token}` expression are
 * only rendered inside the user dropdown, which is hidden when `currentUserEmail` is null).
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@WebMvcTest(controllers = [NotesViewController::class])
@AutoConfigureMockMvc(addFilters = false)
class NotesViewControllerMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var findNoteUseCase: FindNoteUseCase

    @MockBean
    private lateinit var listNotesUseCase: ListNotesUseCase

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    // ── Unauthenticated ──────────────────────────────────────────────────────

    @Test
    fun `GET notes directory returns 200 with HTML content`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }

    @Test
    fun `GET notes directory includes breadcrumb with Home and Contents links`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("href=\"/\"")))
            .andExpect(content().string(containsString("href=\"/contents\"")))
    }

    @Test
    fun `GET notes by title returns 200 with rendered note content`() {
        val author = randomUser()
        val note = randomNote(authorId = author.id, title = "hello-world")
        val result = FindNoteUseCase.Result(note, "# Hello")
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("hello-world", null)).willReturn(result)

        mockMvc.perform(get("/notes/hello-world"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("hello-world")))
    }

    @Test
    fun `GET notes by title returns 404 when note does not exist`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("unknown-note", null))
            .willThrow(NoteNotFoundException("unknown-note"))

        mockMvc.perform(get("/notes/unknown-note"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET notes by title returns 403 when access is denied`() {
        val noteId = NoteId.generate()
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("private-note", null))
            .willThrow(NoteAccessDeniedException(noteId))

        mockMvc.perform(get("/notes/private-note"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET notes with blank trailing path falls back to directory`() {
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())

        mockMvc.perform(get("/notes/"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }

    @Test
    fun `GET notes with unsupported locale falls back to English directory`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())

        mockMvc.perform(get("/notes").locale(java.util.Locale.JAPANESE))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }

    // ── Authenticated ────────────────────────────────────────────────────────

    @Test
    fun `GET notes directory with authenticated user passes user ID to use case`() {
        val userId = UserId.generate()
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, userId)).willReturn(emptyMap())
        authenticate(userId)

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }

    @Test
    fun `GET note by title shows edit button when requester is the author`() {
        val author = randomUser()
        val note = randomNote(authorId = author.id, title = "hello-world")
        val result = FindNoteUseCase.Result(note, "# Hello")
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("hello-world", author.id)).willReturn(result)
        authenticate(author.id)

        mockMvc.perform(get("/notes/hello-world"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("note-edit-btn")))
    }

    @Test
    fun `GET note by title hides edit button when requester is not the author`() {
        val author = randomUser()
        val reader = randomUser()
        val note = randomNote(authorId = author.id, title = "hello-world")
        val result = FindNoteUseCase.Result(note, "# Hello")
        given(listNotesUseCase.listByLanguage(anyArg(), anyArg())).willReturn(emptyMap())
        given(findNoteUseCase.findByTitle("hello-world", reader.id)).willReturn(result)
        authenticate(reader.id)

        mockMvc.perform(get("/notes/hello-world"))
            .andExpect(status().isOk)
            .andExpect(content().string(not(containsString("note-edit-btn"))))
    }

    // ── resolveCurrentUserId edge cases ──────────────────────────────────────

    @Test
    fun `GET notes with unauthenticated auth object treats as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        // 2-arg constructor → isAuthenticated = false
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("some-principal", null)

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with anonymous user principal treats as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("anonymousUser", null, emptyList())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with non-UUID principal treats as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("not-a-uuid", null, emptyList())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with non-String principal treats as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(42, null, emptyList())

        mockMvc.perform(get("/notes"))
            .andExpect(status().isOk)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun authenticate(userId: UserId) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId.value.toString(), null, emptyList())
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> anyArg(): T = Mockito.any<Any>() as T
    }
}
