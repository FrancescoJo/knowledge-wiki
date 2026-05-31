/*
 * NoteWriteControllerMvcTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package testcase.medium.api.endpoint.note.impl

import com.fj.omnimemo.api.endpoint.ApiPathsV1
import com.fj.omnimemo.api.endpoint.note.impl.NoteWriteControllerImpl
import com.fj.omnimemo.core.note.exception.DuplicateNoteTitleException
import com.fj.omnimemo.core.note.exception.NoteAlreadyDeletedException
import com.fj.omnimemo.core.note.exception.NoteNotFoundException
import com.fj.omnimemo.core.note.exception.StaleNoteVersionException
import com.fj.omnimemo.core.note.model.*
import com.fj.omnimemo.core.note.usecase.CreateNoteUseCase
import com.fj.omnimemo.core.note.usecase.SoftDeleteNoteUseCase
import com.fj.omnimemo.core.note.usecase.UpdateNoteUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import test.com.fj.omnimemo.core.annotation.MediumTest
import java.time.Instant
import java.util.*

/**
 * Medium Tests for [NoteWriteControllerImpl]: verifies HTTP routing, JSON serialisation,
 * and status codes for write operations via the Spring MVC stack.
 *
 * Read operations are covered in [NoteControllerMvcTest].
 * Security filters are excluded; authentication logic is covered in
 * [com.fj.omnimemo.api.security.JwtAuthenticationFilterTest].
 *
 * Kotlin value classes are inlined at non-null JVM call sites, so stubs use
 * concrete argument values (not anyArg()) to avoid NPE before Mockito intercepts.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
@MediumTest
@WebMvcTest(controllers = [NoteWriteControllerImpl::class])
@AutoConfigureMockMvc(addFilters = false)
class NoteWriteControllerMvcTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var createNoteUseCase: CreateNoteUseCase

    @MockBean
    private lateinit var updateNoteUseCase: UpdateNoteUseCase

    @MockBean
    private lateinit var softDeleteNoteUseCase: SoftDeleteNoteUseCase

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
    fun `POST notes returns 201 with created note`() {
        // MockMvc uses "127.0.0.1" as the default remote address.
        given(
            createNoteUseCase.create(
                authorId = authorId,
                language = NoteLanguage.EN,
                title = "Getting Started",
                content = "# Hello",
                accessLevel = NoteAccessLevel.PUBLIC,
                status = NoteStatus.PUBLISHED,
                remoteIp = "127.0.0.1",
                summary = null,
            )
        ).willReturn(existingNote)

        mockMvc.perform(
            post(ApiPathsV1.NOTES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"language":"en","title":"Getting Started","content":"# Hello",""" +
                            """"accessLevel":"PUBLIC","status":"PUBLISHED"}"""
                )
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.body.title").value("Getting Started"))
            .andExpect(jsonPath("$.body.id").isString)
    }

    @Test
    fun `PUT notes by id returns 200 with updated note`() {
        given(
            updateNoteUseCase.update(
                noteId = noteId,
                editorId = authorId,
                expectedVersion = 1,
                title = null,
                content = "updated content",
                accessLevel = null,
                status = null,
                remoteIp = "127.0.0.1",
                summary = null,
            )
        ).willReturn(existingNote)

        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/${noteId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedVersion":1,"content":"updated content"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.body.title").value("Getting Started"))
    }

    @Test
    fun `PUT notes by id returns 404 when note is not found`() {
        val unknownId = NoteId(UUID.randomUUID())
        given(
            updateNoteUseCase.update(
                noteId = unknownId,
                editorId = authorId,
                expectedVersion = 1,
                title = null,
                content = "updated content",
                accessLevel = null,
                status = null,
                remoteIp = "127.0.0.1",
                summary = null,
            )
        ).willThrow(NoteNotFoundException(unknownId))

        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/${unknownId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedVersion":1,"content":"updated content"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT notes by id returns 400 for malformed UUID`() {
        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/not-a-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedVersion":1,"content":"updated content"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `DELETE notes by id returns 204`() {
        mockMvc.perform(delete("${ApiPathsV1.NOTES}/${noteId.value}"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE notes by id returns 400 for malformed UUID`() {
        mockMvc.perform(delete("${ApiPathsV1.NOTES}/not-a-uuid"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST notes returns 409 for duplicate title`() {
        given(
            createNoteUseCase.create(
                authorId = authorId,
                language = NoteLanguage.EN,
                title = "Getting Started",
                content = "# Hello",
                accessLevel = NoteAccessLevel.PUBLIC,
                status = NoteStatus.PUBLISHED,
                remoteIp = "127.0.0.1",
                summary = null,
            )
        ).willThrow(DuplicateNoteTitleException("Getting Started"))

        mockMvc.perform(
            post(ApiPathsV1.NOTES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"language":"en","title":"Getting Started","content":"# Hello",""" +
                            """"accessLevel":"PUBLIC","status":"PUBLISHED"}"""
                )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `PUT notes by id returns 409 for stale version`() {
        given(
            updateNoteUseCase.update(
                noteId = noteId,
                editorId = authorId,
                expectedVersion = 1,
                title = null,
                content = "updated content",
                accessLevel = null,
                status = null,
                remoteIp = "127.0.0.1",
                summary = null,
            )
        ).willThrow(StaleNoteVersionException(noteId, expected = 1, actual = 3))

        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/${noteId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedVersion":1,"content":"updated content"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `DELETE notes by id returns 409 when note is already deleted`() {
        given(softDeleteNoteUseCase.softDelete(noteId, authorId, "127.0.0.1"))
            .willThrow(NoteAlreadyDeletedException(noteId))

        mockMvc.perform(delete("${ApiPathsV1.NOTES}/${noteId.value}"))
            .andExpect(status().isConflict)
    }

    @Test
    fun `POST notes returns 400 for unknown access level`() {
        mockMvc.perform(
            post(ApiPathsV1.NOTES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"language":"en","title":"Test","content":"# Hello",""" +
                            """"accessLevel":"INVALID","status":"PUBLISHED"}"""
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST notes returns 400 for unknown status`() {
        mockMvc.perform(
            post(ApiPathsV1.NOTES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"language":"en","title":"Test","content":"# Hello",""" +
                            """"accessLevel":"PUBLIC","status":"INVALID"}"""
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT notes by id with explicit access level and status returns 200`() {
        given(
            updateNoteUseCase.update(
                noteId = noteId,
                editorId = authorId,
                expectedVersion = 1,
                title = null,
                content = "updated content",
                accessLevel = NoteAccessLevel.PRIVATE,
                status = NoteStatus.DRAFT,
                remoteIp = "127.0.0.1",
                summary = null,
            )
        ).willReturn(existingNote)

        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/${noteId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"expectedVersion":1,"content":"updated content",""" +
                            """"accessLevel":"PRIVATE","status":"DRAFT"}"""
                )
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `PUT notes by id returns 400 for unknown access level`() {
        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/${noteId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedVersion":1,"content":"updated","accessLevel":"INVALID"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT notes by id returns 400 for unknown status`() {
        mockMvc.perform(
            put("${ApiPathsV1.NOTES}/${noteId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expectedVersion":1,"content":"updated","status":"INVALID"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST notes returns 401 when requester is not authenticated`() {
        SecurityContextHolder.clearContext()

        mockMvc.perform(
            post(ApiPathsV1.NOTES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"language":"en","title":"Test","content":"# Hello",""" +
                            """"accessLevel":"PUBLIC","status":"PUBLISHED"}"""
                )
        )
            .andExpect(status().isUnauthorized)
    }
}
