/*
 * NoteControllerMvcTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.medium.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.note.impl.NoteControllerImpl
import com.fj.omnimemo.core.note.exception.NoteAccessDeniedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.usecase.FindNoteUseCase
import com.fj.omnimemo.core.note.usecase.ListNotesUseCase
import com.fj.omnimemo.core.user.UserProfileCache
import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.infrastructure.security.JwtTokenService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import test.com.fj.omnimemo.core.annotation.MediumTest
import java.time.Instant
import java.util.*

/**
 * Medium Tests for [NoteControllerImpl]: verifies HTTP routing, JSON serialisation,
 * and status codes for read operations via the Spring MVC stack.
 *
 * Write operations are covered in [NoteWriteControllerMvcTest].
 * Security filters are excluded; authentication logic is covered in
 * [com.fj.omnimemo.api.security.JwtAuthenticationFilterTest].
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@WebMvcTest(controllers = [NoteControllerImpl::class])
@AutoConfigureMockMvc(addFilters = false)
class NoteControllerMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var findNoteUseCase: FindNoteUseCase

    @MockBean
    private lateinit var listNotesUseCase: ListNotesUseCase

    // Required by GlobalModelAdvice; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var userProfileCache: UserProfileCache

    // Required by SecurityConfiguration; not referenced in test methods directly.
    @Suppress("UnusedPrivateProperty")
    @MockBean
    private lateinit var jwtTokenService: JwtTokenService

    private val noteId = NoteId(UUID.randomUUID())
    private val authorId = UserId(UUID.randomUUID())
    private val now = Instant.parse("2026-05-30T00:00:00Z")

    private val existingNote = Note.reconstitute(
        id = noteId,
        language = NoteLanguage.EN,
        title = "Getting Started",
        titleIndex = "G",
        accessLevel = NoteAccessLevel.PUBLIC,
        status = NoteStatus.PUBLISHED,
        currentVersion = 1,
        authorId = authorId,
        softDeletedBy = null,
        softDeletedAt = null,
        createdAt = now,
        updatedAt = now,
    )

    @BeforeEach
    fun setUp() {
        val auth = UsernamePasswordAuthenticationToken(authorId.value.toString(), null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `GET notes returns 200 with notes grouped by title index`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, authorId))
            .willReturn(mapOf("G" to listOf(existingNote)))

        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "en"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.body.G[0].title").value("Getting Started"))
            .andExpect(jsonPath("$.body.G[0].titleIndex").value("G"))
            .andExpect(jsonPath("$.body.G[0].accessLevel").value("PUBLIC"))
    }

    @Test
    fun `GET notes returns 400 for unknown language`() {
        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "zz"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET notes by id returns 200 with note body`() {
        given(findNoteUseCase.findById(noteId, authorId))
            .willReturn(FindNoteUseCase.Result(existingNote, "# Hello"))

        mockMvc.perform(get("${ApiPathsV1.NOTES}/${noteId.value}"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.body.id").value(noteId.value.toString()))
            .andExpect(jsonPath("$.body.title").value("Getting Started"))
            .andExpect(jsonPath("$.body.content").value("# Hello"))
            .andExpect(jsonPath("$.body.language").value("en"))
            .andExpect(jsonPath("$.body.authorId").value(authorId.value.toString()))
    }

    @Test
    fun `GET notes by id returns 404 when note is not found`() {
        val unknownId = NoteId(UUID.randomUUID())
        given(findNoteUseCase.findById(unknownId, authorId))
            .willThrow(NoteNotFoundException(unknownId))

        mockMvc.perform(get("${ApiPathsV1.NOTES}/${unknownId.value}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET notes by id returns 403 when access is denied`() {
        given(findNoteUseCase.findById(noteId, authorId))
            .willThrow(NoteAccessDeniedException(noteId))

        mockMvc.perform(get("${ApiPathsV1.NOTES}/${noteId.value}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET notes by id returns 400 for malformed UUID`() {
        mockMvc.perform(get("${ApiPathsV1.NOTES}/not-a-uuid"))
            .andExpect(status().isBadRequest)
    }

    // ── resolveRequesterId edge cases ────────────────────────────────────────

    @Test
    fun `GET notes with null authentication treats requester as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.clearContext()

        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "en"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with unauthenticated token treats requester as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("some-principal", null)

        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "en"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with anonymous user principal treats requester as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("anonymousUser", null, emptyList())

        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "en"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with non-String principal treats requester as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(42, null, emptyList())

        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "en"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET notes with non-UUID string principal treats requester as anonymous`() {
        given(listNotesUseCase.listByLanguage(NoteLanguage.EN, null)).willReturn(emptyMap())
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("not-a-uuid", null, emptyList())

        mockMvc.perform(get(ApiPathsV1.NOTES).param("language", "en"))
            .andExpect(status().isOk)
    }
}
