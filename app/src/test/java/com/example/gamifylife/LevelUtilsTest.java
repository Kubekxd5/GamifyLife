package com.example.gamifylife.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class LevelUtilsTest {

    @Test
    public void getXPForNextLevel_levelZeroOrLess_returnsBaseXP() {
        assertEquals(100, LevelUtils.getXPForNextLevel(0));
        assertEquals(100, LevelUtils.getXPForNextLevel(-5));
    }

    @Test
    public void getXPForNextLevel_levelOne_returnsBaseXP() {
        assertEquals(100, LevelUtils.getXPForNextLevel(1));
    }

    @Test
    public void getXPForNextLevel_positiveLevel_returnsCorrectXP() {
        assertEquals(200, LevelUtils.getXPForNextLevel(2));
        assertEquals(500, LevelUtils.getXPForNextLevel(5));
        assertEquals(1000, LevelUtils.getXPForNextLevel(10));
    }

    @Test
    public void getTotalXPForLevel_targetLevelOneOrLess_returnsZero() {
        assertEquals(0, LevelUtils.getTotalXPForLevel(1));
        assertEquals(0, LevelUtils.getTotalXPForLevel(0));
        assertEquals(0, LevelUtils.getTotalXPForLevel(-5));
    }

    @Test
    public void getTotalXPForLevel_targetLevelTwo_returnsXPForLevelOne() {
        assertEquals(100, LevelUtils.getTotalXPForLevel(2));
    }

    @Test
    public void getTotalXPForLevel_positiveTargetLevel_returnsCorrectTotalXP() {
        assertEquals(300, LevelUtils.getTotalXPForLevel(3));
        assertEquals(600, LevelUtils.getTotalXPForLevel(4));
        assertEquals(1000, LevelUtils.getTotalXPForLevel(5));
    }

    @Test
    public void calculateLevel_zeroXP_returnsLevelOne() {
        assertEquals(1, LevelUtils.calculateLevel(0));
    }

    @Test
    public void calculateLevel_lessThanXPForLevelTwo_returnsLevelOne() {
        assertEquals(1, LevelUtils.calculateLevel(50));
        assertEquals(1, LevelUtils.calculateLevel(99));
    }

    @Test
    public void calculateLevel_exactXPForLevelTwo_returnsLevelTwo() {
        assertEquals(2, LevelUtils.calculateLevel(100));
    }

    @Test
    public void calculateLevel_xpForHigherLevels_returnsCorrectLevel() {
        assertEquals(2, LevelUtils.calculateLevel(299));
        assertEquals(3, LevelUtils.calculateLevel(300));
        assertEquals(3, LevelUtils.calculateLevel(599));
        assertEquals(4, LevelUtils.calculateLevel(600));
        assertEquals(5, LevelUtils.calculateLevel(1000));
        assertEquals(5, LevelUtils.calculateLevel(1050));
    }

    @Test
    public void getXPProgressInCurrentLevel_levelOne_returnsTotalXp() {
        assertEquals(0, LevelUtils.getXPProgressInCurrentLevel(0));
        assertEquals(50, LevelUtils.getXPProgressInCurrentLevel(50));
        assertEquals(99, LevelUtils.getXPProgressInCurrentLevel(99));
    }

    @Test
    public void getXPProgressInCurrentLevel_levelTwoExactly_returnsZero() {
        assertEquals(0, LevelUtils.getXPProgressInCurrentLevel(100));
    }

    @Test
    public void getXPProgressInCurrentLevel_higherLevels_returnsCorrectProgress() {
        assertEquals(50, LevelUtils.getXPProgressInCurrentLevel(350));
        assertEquals(100, LevelUtils.getXPProgressInCurrentLevel(700));
    }

    @Test
    public void getXPTotalForCurrentLevelBar_levelOne_returnsXPForLevelOne() {
        assertEquals(100, LevelUtils.getXPTotalForCurrentLevelBar(0));
        assertEquals(100, LevelUtils.getXPTotalForCurrentLevelBar(50));
    }

    @Test
    public void getXPTotalForCurrentLevelBar_higherLevels_returnsXPForThatLevel() {
        assertEquals(200, LevelUtils.getXPTotalForCurrentLevelBar(150));
        assertEquals(300, LevelUtils.getXPTotalForCurrentLevelBar(300));
    }
}