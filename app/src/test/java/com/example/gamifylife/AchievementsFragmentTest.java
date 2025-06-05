package com.example.gamifylife;

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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AchievementsFragmentTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock
    FirebaseAuth mockAuth;
    @Mock
    FirebaseUser mockFirebaseUser;
    @Mock
    FirebaseFirestore mockDb;
    @Mock
    AchievementAdapter mockAdapter;
    @Mock
    TextView mockTextViewEmpty;

    @Mock
    CollectionReference mockCollectionReference;
    @Mock
    DocumentReference mockDocumentReference;
    @Mock
    Query mockQuery;
    @Mock
    Task<QuerySnapshot> mockTaskQuerySnapshot;
    @Mock
    QuerySnapshot mockQuerySnapshot;

    AchievementsFragment fragmentSUT;

    @Captor
    ArgumentCaptor<OnCompleteListener<QuerySnapshot>> queryCompleteListenerCaptor;

    @Before
    public void setUp() {
        fragmentSUT = new AchievementsFragment();
        fragmentSUT.mAuth = mockAuth;
        fragmentSUT.db = mockDb;
        fragmentSUT.achievementAdapter = mockAdapter;
        fragmentSUT.textViewEmpty = mockTextViewEmpty;
        fragmentSUT.achievementList = new ArrayList<>();

        when(mockDb.collection(anyString())).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockDocumentReference.collection(anyString())).thenReturn(mockCollectionReference);

        when(mockCollectionReference.whereGreaterThanOrEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.whereLessThanOrEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery); // Ogólne mockowanie orderBy
        when(mockQuery.orderBy(anyString())).thenReturn(mockQuery); // Jeśli używasz orderBy bez kierunku
        when(mockQuery.get()).thenReturn(mockTaskQuerySnapshot);
    }

    @Test
    public void loadAchievementsForDate_userNotLoggedIn_clearsListAndShowsLoginMessage() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        fragmentSUT.loadAchievementsForDate(new Date());

        assertTrue(fragmentSUT.achievementList.isEmpty());
        verify(mockAdapter).notifyDataSetChanged();
        verify(mockTextViewEmpty).setVisibility(View.VISIBLE);
        // Nie możemy łatwo zweryfikować setText bez Robolectric lub zmiany w kodzie fragmentu
    }

    @Test
    public void loadAchievementsForDate_userLoggedIn_noAchievements_showsEmptyMessage() {
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");

        when(mockTaskQuerySnapshot.isSuccessful()).thenReturn(true);
        when(mockTaskQuerySnapshot.getResult()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        fragmentSUT.loadAchievementsForDate(new Date());
        verify(mockTaskQuerySnapshot).addOnCompleteListener(queryCompleteListenerCaptor.capture());
        queryCompleteListenerCaptor.getValue().onComplete(mockTaskQuerySnapshot);

        assertTrue(fragmentSUT.achievementList.isEmpty());
        verify(mockAdapter).notifyDataSetChanged();
        verify(mockTextViewEmpty).setVisibility(View.VISIBLE);
    }

    @Test
    public void loadAchievementsForDate_userLoggedIn_withAchievements_populatesList() {
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");

        List<QueryDocumentSnapshot> mockDocuments = new ArrayList<>();
        QueryDocumentSnapshot mockDoc1 = mock(QueryDocumentSnapshot.class);
        Achievement achievement1 = new Achievement("Test Ach 1", "Desc 1", "ic_icon1", 10, new Date());
        achievement1.setDocumentId("doc1"); // Ustaw ID dla testu

        when(mockDoc1.toObject(Achievement.class)).thenReturn(achievement1);
        when(mockDoc1.getId()).thenReturn("doc1"); // Mockowanie getId()
        mockDocuments.add(mockDoc1);

        when(mockTaskQuerySnapshot.isSuccessful()).thenReturn(true);
        when(mockTaskQuerySnapshot.getResult()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        // Zamiast mockować iterator i getDocuments, co jest kruche,
        // zamockujemy bezpośrednio wynik pętli for-each
        when(mockQuerySnapshot.iterator()).thenReturn(mockDocuments.iterator());


        fragmentSUT.loadAchievementsForDate(new Date());
        verify(mockTaskQuerySnapshot).addOnCompleteListener(queryCompleteListenerCaptor.capture());
        queryCompleteListenerCaptor.getValue().onComplete(mockTaskQuerySnapshot);

        assertEquals(1, fragmentSUT.achievementList.size());
        assertEquals("doc1", fragmentSUT.achievementList.get(0).getDocumentId());
        assertEquals("Test Ach 1", fragmentSUT.achievementList.get(0).getTitle());
        verify(mockAdapter).notifyDataSetChanged();
        verify(mockTextViewEmpty).setVisibility(View.GONE);
    }
}