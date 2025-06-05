package com.example.gamifylife;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.gamifylife.models.Achievement;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AchievementsFragmentTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock FirebaseAuth mockAuth;
    @Mock FirebaseUser mockFirebaseUser;
    @Mock FirebaseFirestore mockDb;
    @Mock AchievementAdapter mockAdapter;
    @Mock TextView mockTextViewEmpty;
    @Mock Context mockContext;
    @Mock Resources mockResources;

    @Mock CollectionReference mockUsersCollection;
    @Mock DocumentReference mockUserDocument;
    @Mock CollectionReference mockAchievementsSubCollection;
    @Mock Query mockFirestoreQuery;
    @Mock Task<QuerySnapshot> mockTaskQuerySnapshot;
    @Mock QuerySnapshot mockQueryResult;

    @Spy
    AchievementsFragment fragmentSUT = new AchievementsFragment();
    List<Achievement> achievementListSUT;

    private MockedStatic<Log> mockedLog;

    @Captor
    ArgumentCaptor<OnCompleteListener<QuerySnapshot>> firestoreCompleteListenerCaptor;

    @Before
    public void setUp() {
        mockedLog = Mockito.mockStatic(Log.class);
        achievementListSUT = new ArrayList<>();

        fragmentSUT.mAuth = mockAuth;
        fragmentSUT.db = mockDb;
        fragmentSUT.achievementAdapter = mockAdapter;
        fragmentSUT.textViewEmpty = mockTextViewEmpty;
        fragmentSUT.achievementList = achievementListSUT;

        doReturn(true).when(fragmentSUT).isAdded();
        doReturn(mockContext).when(fragmentSUT).getContext();
        doReturn(mockResources).when(fragmentSUT).getResources();

        when(mockResources.getString(R.string.please_log_in)).thenReturn("Test Please Log In");
        when(mockResources.getString(R.string.no_achievements_for_date)).thenReturn("Test No Achievements");
        when(mockResources.getString(R.string.error_loading_achievements)).thenReturn("Test Error Loading");

        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocument);
        when(mockUserDocument.collection("achievements")).thenReturn(mockAchievementsSubCollection);

        when(mockAchievementsSubCollection.whereGreaterThanOrEqualTo(anyString(), any(Date.class))).thenReturn(mockFirestoreQuery);
        when(mockFirestoreQuery.whereLessThanOrEqualTo(anyString(), any(Date.class))).thenReturn(mockFirestoreQuery);
        when(mockFirestoreQuery.orderBy(eq("targetDate"), any(Query.Direction.class))).thenReturn(mockFirestoreQuery);
        when(mockFirestoreQuery.orderBy(eq("createdAt"), any(Query.Direction.class))).thenReturn(mockFirestoreQuery);
        when(mockFirestoreQuery.get()).thenReturn(mockTaskQuerySnapshot);

        when(mockTaskQuerySnapshot.addOnCompleteListener(any(OnCompleteListener.class)))
                .thenReturn(mockTaskQuerySnapshot);
    }

    @After
    public void tearDown() {
        if (mockedLog != null) {
            mockedLog.close();
        }
    }

    private void simulateFirestoreCallback(Task<QuerySnapshot> taskToReturnFromCallback) {
        // Przechwyć listener, który został przekazany do addOnCompleteListener w kodzie produkcyjnym
        verify(mockTaskQuerySnapshot).addOnCompleteListener(firestoreCompleteListenerCaptor.capture());
        // Wywołaj przechwycony listener
        firestoreCompleteListenerCaptor.getValue().onComplete(taskToReturnFromCallback);
    }

    @Test
    public void loadAchievementsForDate_userNotLoggedIn_updatesUiCorrectly() {
        Mockito.reset(mockTextViewEmpty, mockAdapter); // Zresetuj interakcje dla tych mocków
        when(mockAuth.getCurrentUser()).thenReturn(null);
        achievementListSUT.add(new Achievement());

        fragmentSUT.loadAchievementsForDate(new Date());

        assertTrue("Achievement list should be empty", achievementListSUT.isEmpty());
        verify(mockAdapter, times(1)).notifyDataSetChanged();
        verify(mockTextViewEmpty).setVisibility(View.VISIBLE);
        verify(mockTextViewEmpty, times(1)).setText("Test Please Log In"); // Oczekujemy jednego wywołania
        verify(mockTaskQuerySnapshot, never()).addOnCompleteListener(any());
    }

    @Test
    public void loadAchievementsForDate_userLoggedIn_noAchievements_updatesUiCorrectly() {
        Mockito.reset(mockTextViewEmpty, mockAdapter);
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");

        when(mockTaskQuerySnapshot.isSuccessful()).thenReturn(true);
        when(mockTaskQuerySnapshot.getResult()).thenReturn(mockQueryResult);
        when(mockQueryResult.isEmpty()).thenReturn(true);
        when(mockQueryResult.iterator()).thenReturn(Collections.emptyIterator());

        fragmentSUT.loadAchievementsForDate(new Date());
        simulateFirestoreCallback(mockTaskQuerySnapshot);

        assertTrue("Achievement list should be empty", achievementListSUT.isEmpty());
        verify(mockAdapter, times(1)).notifyDataSetChanged();
        verify(mockTextViewEmpty).setVisibility(View.VISIBLE);
        verify(mockTextViewEmpty, times(1)).setText("Test No Achievements");
    }


    @Test
    public void loadAchievementsForDate_userLoggedIn_withAchievements_populatesListAndHidesEmptyView() {
        Mockito.reset(mockTextViewEmpty, mockAdapter);
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");

        List<QueryDocumentSnapshot> mockDocumentList = new ArrayList<>();
        QueryDocumentSnapshot mockDoc1 = mock(QueryDocumentSnapshot.class);
        Achievement achievement1Data = new Achievement("Test Ach 1", "Desc 1", "ic_icon1", 10, new Date());
        when(mockDoc1.toObject(Achievement.class)).thenReturn(achievement1Data);
        when(mockDoc1.getId()).thenReturn("doc1FirebaseId");
        mockDocumentList.add(mockDoc1);

        when(mockTaskQuerySnapshot.isSuccessful()).thenReturn(true);
        when(mockTaskQuerySnapshot.getResult()).thenReturn(mockQueryResult);
        when(mockQueryResult.isEmpty()).thenReturn(false);
        when(mockQueryResult.iterator()).thenReturn(mockDocumentList.iterator());

        fragmentSUT.loadAchievementsForDate(new Date());
        simulateFirestoreCallback(mockTaskQuerySnapshot);

        assertEquals("Achievement list should contain 1 item", 1, achievementListSUT.size());
        assertEquals("doc1FirebaseId", achievementListSUT.get(0).getDocumentId());
        verify(mockAdapter, times(1)).notifyDataSetChanged(); // Raz na początku, raz w onComplete
        verify(mockTextViewEmpty).setVisibility(View.GONE);
    }

    @Test
    public void loadAchievementsForDate_firestoreTaskFails_showsErrorAndClearsList() {
        Mockito.reset(mockTextViewEmpty, mockAdapter);
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");

        when(mockTaskQuerySnapshot.isSuccessful()).thenReturn(false);

        // Użyj generycznego wyjątku zamiast FirebaseFirestoreException
        Exception genericException = new RuntimeException("Test Firestore error (simulated)");
        when(mockTaskQuerySnapshot.getException()).thenReturn(genericException);

        achievementListSUT.add(new Achievement());

        fragmentSUT.loadAchievementsForDate(new Date());
        simulateFirestoreCallback(mockTaskQuerySnapshot);

        assertTrue("Achievement list should be empty after error", achievementListSUT.isEmpty());
        verify(mockAdapter, times(1)).notifyDataSetChanged();
        verify(mockTextViewEmpty).setVisibility(View.VISIBLE);
        verify(mockTextViewEmpty, times(1)).setText("Test Error Loading");
    }
}