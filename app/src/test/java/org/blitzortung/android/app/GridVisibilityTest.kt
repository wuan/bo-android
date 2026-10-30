package org.blitzortung.android.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GridVisibilityTest {
    @Test
    fun gridShownByDefault() {
        assertThat(shouldShowGrid(hideGrid = false, showClusters = false, clustersSupported = true)).isTrue
    }

    @Test
    fun gridShownWhenClustersOffEvenIfHideGridRequested() {
        assertThat(shouldShowGrid(hideGrid = true, showClusters = false, clustersSupported = true)).isTrue
    }

    @Test
    fun gridShownWhenClustersUnsupportedEvenIfHideGridRequested() {
        assertThat(shouldShowGrid(hideGrid = true, showClusters = true, clustersSupported = false)).isTrue
    }

    @Test
    fun gridHiddenWhenClustersShownOnSupportedProviderAndHideGridRequested() {
        assertThat(shouldShowGrid(hideGrid = true, showClusters = true, clustersSupported = true)).isFalse
    }

    @Test
    fun gridShownWhenHideGridNotRequested() {
        assertThat(shouldShowGrid(hideGrid = false, showClusters = true, clustersSupported = true)).isTrue
    }
}
