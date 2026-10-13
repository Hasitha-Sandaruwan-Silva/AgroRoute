package com.agroroute.features.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.LoadingButton
import com.github.thelacspace.icons.Lucide

/**
 * Screen for submitting a rating and review for a completed order.
 */
@Composable
fun SubmitReviewScreen(
    orderId: String,
    revieweeId: String,
    revieweeName: String,
    navController: NavController,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(orderId) {
        viewModel.checkHasReviewed(orderId, revieweeId)
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    LaunchedEffect(uiState.submissionSuccess) {
        if (uiState.submissionSuccess) {
            snackbarHostState.showSnackbar("Review submitted successfully!")
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ReviewHeader(revieweeName = revieweeName)
            if (uiState.isChecking || uiState.hasReviewed || uiState.error != null)
                com.agroroute.core.components.ReviewEligibilitySection(uiState.isChecking, false,
                uiState.hasReviewed, uiState.error, revieweeName, {},
                { viewModel.checkHasReviewed(orderId, revieweeId) })
            if (!uiState.isChecking && !uiState.canReview && !uiState.hasReviewed && uiState.error == null)
                Text("This order is not eligible for a review.")
            Spacer(Modifier.height(16.dp))
            RatingCard(
                rating = rating,
                onRatingChange = { rating = it }
            )
            Spacer(Modifier.height(16.dp))
            CommentCard(
                comment = comment,
                onCommentChange = { comment = it }
            )
            Spacer(Modifier.height(24.dp))
            LoadingButton(
                text = "Submit Review",
                isLoading = uiState.isLoading,
                enabled = rating in 1..5 && comment.isNotBlank() && uiState.canReview && !uiState.isChecking && !uiState.hasReviewed,
                onClick = {
                    viewModel.submitReview(
                        orderId = orderId,
                        revieweeId = revieweeId,
                        rating = rating,
                        comment = comment
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReviewHeader(revieweeName: String) {
    Text("Review $revieweeName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Share your experience with this delivered order.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RatingCard(rating: Int, onRatingChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Rating",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { star ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (star <= rating)
                                LeafAccent.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onRatingChange(star) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (star <= rating) "★" else "☆",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (star <= rating) LeafAccent else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (rating) {
                0 -> "Tap a star to rate"
                1 -> "Poor"
                2 -> "Fair"
                3 -> "Good"
                4 -> "Very Good"
                5 -> "Excellent"
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommentCard(comment: String, onCommentChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Write a Review",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            placeholder = { Text("Share your experience...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )
    }
}
