package com.group_finity.mascot.mac.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;

import com.group_finity.mascot.mac.jna.ProcessSerialNumber;
import com.group_finity.mascot.mac.jna.AXUIElementRef;
import com.group_finity.mascot.mac.jna.CFStringRef;
import com.group_finity.mascot.mac.jna.CFTypeRef;


public interface Carbon extends Library {
	Carbon INSTANCE = (Carbon) Native.loadLibrary("Carbon", Carbon.class);

	long GetFrontProcess(ProcessSerialNumber psn);
	long GetProcessPID(final ProcessSerialNumber psn, LongByReference pidp);

	long AXUIElementCopyAttributeValue(
		AXUIElementRef element, CFStringRef attr, PointerByReference value);
	int AXUIElementSetAttributeValue(
		AXUIElementRef element, CFStringRef attr, CFTypeRef value);
	AXUIElementRef AXUIElementCreateApplication(long pid);
	boolean AXValueGetValue(AXValueRef value, long type, Pointer valuep);
	AXValueRef AXValueCreate(long type, Pointer valuep);

	CFTypeRef CFPreferencesCopyValue(
		CFStringRef key, CFStringRef app, Pointer user, Pointer host);
	boolean CFPreferencesAppSynchronize(CFStringRef app);
	boolean CFNumberGetValue(CFTypeRef number, long type, ByReference ref);
	CFStringRef CFStringCreateWithCharacters(Pointer alloc, char[] source, long length);
	boolean CFStringGetCString(
		CFTypeRef strRef, Pointer buffer, long length, int encoding);
	int CFStringGetSystemEncoding();

	long CFArrayGetCount(CFArrayRef array);
	Pointer CFArrayGetValueAtIndex(CFArrayRef array, long i);

	void CFRetain(CFTypeRef any);
	void CFRelease(CFTypeRef any);
	void CFShow(CFTypeRef any);

	NativeLibrary nl = NativeLibrary.getProcess();
	Pointer kCurrentUser = nl.getGlobalVariableAddress("kCFPreferencesCurrentUser").getPointer(0);
	Pointer kAnyHost = nl.getGlobalVariableAddress("kCFPreferencesAnyHost").getPointer(0);

	static final long kAXErrorSuccess = 0;
	static final long
	  kAXValueCGPointType = 1,
		kAXValueCGSizeType = 2;
	static final long
  	kCFNumberInt32Type = 3;
}
