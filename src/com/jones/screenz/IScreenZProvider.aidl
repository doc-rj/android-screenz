// AIDL file specifying interface used by clients to retrieve screenshots

package com.jones.screenz;

interface IScreenZProvider {
    // checks if screenshots are available
	boolean isAvailable();

	// takes screenshot and returns path to image file
	String takeScreenshot();
}