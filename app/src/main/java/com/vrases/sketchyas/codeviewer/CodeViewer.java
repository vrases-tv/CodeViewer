package com.vrases.sketchyas.codeviewer;

/*
Copyright ©️ by VRASES • SKETCHYAS 2025
https://play.google.com/store/apps/dev?id=4905832401145871682

*/


import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.view.ViewParent;


/**
* A professional code viewer that extends TextView with syntax highlighting,
* line numbers, typing animation, and other advanced features.
*/
public class CodeViewer extends TextView {
	
	// Theme constants
	public static final int THEME_LIGHT = 0;
	public static final int THEME_DARK = 1;
	public static final int THEME_MONOKAI = 2;
	public static final int THEME_GITHUB = 3;
	public static final int THEME_DRACULA = 4;
	public static final int THEME_SOLARIZED_LIGHT = 5;
	public static final int THEME_SOLARIZED_DARK = 6;
	public static final int THEME_NORD = 7;
	public static final int THEME_NIGHT_OWL = 8;
	public static final int THEME_MATERIAL = 9;
	
	// Animation speeds
	public static final int TYPING_SPEED_SLOW = 50;
	public static final int TYPING_SPEED_MEDIUM = 20;
	public static final int TYPING_SPEED_FAST = 5;
	
	// Default values
	private static final int DEFAULT_LINE_NUMBER_PADDING = 10;
	private static final int DEFAULT_THEME = THEME_LIGHT;
	private static final boolean DEFAULT_SHOW_LINE_NUMBERS = true;
	private static final int DEFAULT_TYPING_SPEED = TYPING_SPEED_MEDIUM;
	
	// Theme colors
	private int mBackgroundColor;
	private int mTextColor;
	private int mLineNumberColor;
	private int mKeywordColor;
	private int mStringColor;
	private int mCommentColor;
	private int mNumberColor;
	private int mOperatorColor;
	private int mAnnotationColor;
	private int mClassColor;
	private int mMethodColor;
	private int mCurrentLineColor;
	private int mSelectionColor;
	private int mGutterBackgroundColor;
	private int mGutterSeparatorColor;
	
	// Configuration
	private boolean mShowLineNumbers = DEFAULT_SHOW_LINE_NUMBERS;
	private int mLineNumberPadding = DEFAULT_LINE_NUMBER_PADDING;
	private int mTheme = DEFAULT_THEME;
	private String mLanguage = "java"; // Default language
	private boolean mEnableHighlighting = true;
	private boolean mEnableLineHighlighting = true;
	private boolean mEnableFolding = true;
	private int mCurrentLine = -1;
	private boolean mIsEditable = false;
	private boolean mAutoIndent = true;
	private boolean mFixedLineNumbers = true; // Always fixed by default
	
	// Typing animation
	private boolean mTypingAnimationEnabled = false;
	private int mTypingSpeed = DEFAULT_TYPING_SPEED;
	private int mTypingPosition = 0;
	private String mFullCode = "";
	private Handler mTypingHandler = new Handler();
	private Runnable mTypingRunnable;
	private ValueAnimator mCursorAnimator;
	private boolean mShowCursor = true;
	private int mCursorColor;
	private int mCursorWidth = 2;
	private int mCursorPosition = 0;
	
	// Syntax highlighting
	private Map<String, Pattern> mPatterns;
	private List<CodeSpan> mCodeSpans = new ArrayList<>();
	private SpannableStringBuilder mSpannableCode = new SpannableStringBuilder();
	private boolean mHighlightingScheduled = false;
	private Handler mHighlightHandler = new Handler();
	private Runnable mHighlightRunnable;
	
	// Line numbers
	private Paint mLineNumberPaint;
	private int mLineNumberWidth;
	private List<Integer> mLineStarts = new ArrayList<>();
	private List<Integer> mFoldedLines = new ArrayList<>();
	private int mScrollY = 0; // Track vertical scroll position
	private int mScrollX = 0; // Track horizontal scroll position
	
	// Scrolling tracking
	private ScrollView mParentScrollView;
	private HorizontalScrollView mParentHorizontalScrollView;
	private ViewTreeObserver.OnScrollChangedListener mScrollListener;
	
	// Gesture detection
	private GestureDetector mGestureDetector;
	
	// Search
	private String mSearchQuery = "";
	private List<Integer> mSearchResults = new ArrayList<>();
	private int mCurrentSearchResult = -1;
	private BackgroundColorSpan mSearchHighlightSpan;
	
	// Code folding
	private Paint mFoldingPaint;
	private int mFoldingIndicatorSize = 10;
	private Map<Integer, Boolean> mFoldableLines = new HashMap<>();
	
	// Line selection
	private int mSelectedLine = -1;
	private Paint mSelectedLinePaint;
	
	public CodeViewer(Context context) {
		super(context);
		init(null);
	}
	
	public CodeViewer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public CodeViewer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	private void init(AttributeSet attrs) {
		// Set default properties for TextView
		setHorizontallyScrolling(true);
		setTextIsSelectable(true); // Ensure text is selectable
		setTypeface(Typeface.MONOSPACE);
		
		// Parse attributes if available
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CodeViewer);
			
			mShowLineNumbers = a.getBoolean(R.styleable.CodeViewer_showLineNumbers, DEFAULT_SHOW_LINE_NUMBERS);
			mLineNumberPadding = a.getDimensionPixelSize(R.styleable.CodeViewer_lineNumberPadding, DEFAULT_LINE_NUMBER_PADDING);
			mTheme = a.getInt(R.styleable.CodeViewer_theme, DEFAULT_THEME);
			mLanguage = a.getString(R.styleable.CodeViewer_language);
			if (mLanguage == null) mLanguage = "java";
			mEnableHighlighting = a.getBoolean(R.styleable.CodeViewer_enableHighlighting, true);
			mEnableLineHighlighting = a.getBoolean(R.styleable.CodeViewer_enableLineHighlighting, true);
			mEnableFolding = a.getBoolean(R.styleable.CodeViewer_enableFolding, true);
			mIsEditable = a.getBoolean(R.styleable.CodeViewer_editable, false);
			mAutoIndent = a.getBoolean(R.styleable.CodeViewer_autoIndent, true);
			mTypingAnimationEnabled = a.getBoolean(R.styleable.CodeViewer_typingAnimation, false);
			mTypingSpeed = a.getInt(R.styleable.CodeViewer_typingSpeed, DEFAULT_TYPING_SPEED);
			mFixedLineNumbers = a.getBoolean(R.styleable.CodeViewer_fixedLineNumbers, true);
			
			String code = a.getString(R.styleable.CodeViewer_code);
			if (code != null) {
				mFullCode = code;
				if (!mTypingAnimationEnabled) {
					setText(code);
				}
			}
			
			a.recycle();
		}
		
		// Apply theme
		applyTheme(mTheme);
		
		// Initialize paints
		mLineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLineNumberPaint.setColor(mLineNumberColor);
		mLineNumberPaint.setTextSize(getTextSize());
		mLineNumberPaint.setTypeface(Typeface.MONOSPACE);
		
		mFoldingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mFoldingPaint.setColor(mLineNumberColor);
		mFoldingPaint.setStyle(Paint.Style.STROKE);
		mFoldingPaint.setStrokeWidth(2);
		
		mSelectedLinePaint = new Paint();
		mSelectedLinePaint.setColor(mCurrentLineColor);
		mSelectedLinePaint.setStyle(Paint.Style.FILL);
		
		// Initialize syntax highlighting patterns
		initPatterns();
		
		// Set up gesture detector
		mGestureDetector = new GestureDetector(getContext(), new GestureListener());
		
		// Set up search highlight span
		mSearchHighlightSpan = new BackgroundColorSpan(0xFFFFFF00); // Yellow highlight
		
		// Set up text change listener for syntax highlighting
		addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// Not used
			}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// Not used
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if (mEnableHighlighting && !mHighlightingScheduled) {
					mHighlightingScheduled = true;
					mHighlightHandler.postDelayed(getHighlightRunnable(), 100); // Debounce
				}
				
				// Update line starts for line numbers
				updateLineStarts();
				
				// Update foldable lines
				if (mEnableFolding) {
					updateFoldableLines();
				}
			}
		});
		
		// Set up typing animation if enabled
		if (mTypingAnimationEnabled && mFullCode.length() > 0) {
			startTypingAnimation();
		}
		
		// Set up cursor animation
		setupCursorAnimation();
		
		// Make editable if specified
		setFocusable(mIsEditable);
		setFocusableInTouchMode(mIsEditable);
		
		// Set padding for line numbers
		updatePadding();
		
		// Set up scroll tracking
		setupScrollTracking();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		// Find parent ScrollViews for scroll tracking
		View parent = (View) getParent();
		while (parent != null) {
			if (parent instanceof ScrollView) {
				mParentScrollView = (ScrollView) parent;
			} else if (parent instanceof HorizontalScrollView) {
				mParentHorizontalScrollView = (HorizontalScrollView) parent;
			}
			
			// FIX: Ensure parent.getParent() is not null before casting
			ViewParent rawParent = parent.getParent();
			if (!(rawParent instanceof View)) { 
				break;  // Stop loop if the parent is null or not a View
			}
			
			parent = (View) rawParent;  // Safe cast after check
		}
		
		// Set up scroll listener
		if (mParentScrollView != null || mParentHorizontalScrollView != null) {
			if (mScrollListener == null) {
				mScrollListener = new ViewTreeObserver.OnScrollChangedListener() {
					@Override
					public void onScrollChanged() {
						if (mParentScrollView != null) {
							mScrollY = mParentScrollView.getScrollY();
						}
						if (mParentHorizontalScrollView != null) {
							mScrollX = mParentHorizontalScrollView.getScrollX();
						}
						invalidate();
					}
				};
			}
			
			ViewTreeObserver observer = getViewTreeObserver();
			if (observer != null) {
				observer.addOnScrollChangedListener(mScrollListener);
			}
		}
	}
	
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		// Remove scroll listener
		if (mScrollListener != null) {
			ViewTreeObserver observer = getViewTreeObserver();
			if (observer != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					observer.removeOnScrollChangedListener(mScrollListener);
				}
			}
		}
	}
	
	private void setupScrollTracking() {
		// We'll implement our own scroll tracking for line numbers
		addOnLayoutChangeListener(new OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, 
			int oldLeft, int oldTop, int oldRight, int oldBottom) {
				updateLineStarts();
				invalidate();
			}
		});
	}
	
	private void applyTheme(int theme) {
		switch (theme) {
			case THEME_DARK:
			mBackgroundColor = 0xFF1E1E1E;
			mTextColor = 0xFFD4D4D4;
			mLineNumberColor = 0xFF858585;
			mKeywordColor = 0xFF569CD6;
			mStringColor = 0xFFCE9178;
			mCommentColor = 0xFF6A9955;
			mNumberColor = 0xFFB5CEA8;
			mOperatorColor = 0xFFD4D4D4;
			mAnnotationColor = 0xFFDCDCAA;
			mClassColor = 0xFF4EC9B0;
			mMethodColor = 0xFFDCDCAA;
			mCurrentLineColor = 0xFF282828;
			mSelectionColor = 0xFF264F78;
			mGutterBackgroundColor = 0xFF252526;
			mGutterSeparatorColor = 0xFF3F3F46;
			mCursorColor = 0xFFAEAFAD;
			break;
			
			case THEME_MONOKAI:
			mBackgroundColor = 0xFF272822;
			mTextColor = 0xFFF8F8F2;
			mLineNumberColor = 0xFF90908A;
			mKeywordColor = 0xFFF92672;
			mStringColor = 0xFFE6DB74;
			mCommentColor = 0xFF75715E;
			mNumberColor = 0xFFAE81FF;
			mOperatorColor = 0xFFF8F8F2;
			mAnnotationColor = 0xFFA6E22E;
			mClassColor = 0xFF66D9EF;
			mMethodColor = 0xFFA6E22E;
			mCurrentLineColor = 0xFF3E3D32;
			mSelectionColor = 0xFF49483E;
			mGutterBackgroundColor = 0xFF2D2E27;
			mGutterSeparatorColor = 0xFF3B3C35;
			mCursorColor = 0xFFF8F8F0;
			break;
			
			case THEME_GITHUB:
			mBackgroundColor = 0xFFFFFFFF;
			mTextColor = 0xFF24292E;
			mLineNumberColor = 0xFFCAD1D8;
			mKeywordColor = 0xFFD73A49;
			mStringColor = 0xFF032F62;
			mCommentColor = 0xFF6A737D;
			mNumberColor = 0xFF005CC5;
			mOperatorColor = 0xFF24292E;
			mAnnotationColor = 0xFF6F42C1;
			mClassColor = 0xFF6F42C1;
			mMethodColor = 0xFF6F42C1;
			mCurrentLineColor = 0xFFFAFBFC;
			mSelectionColor = 0xFFADD6FF;
			mGutterBackgroundColor = 0xFFFAFBFC;
			mGutterSeparatorColor = 0xFFEAECEF;
			mCursorColor = 0xFF24292E;
			break;
			
			case THEME_DRACULA:
			mBackgroundColor = 0xFF282A36;
			mTextColor = 0xFFF8F8F2;
			mLineNumberColor = 0xFF6272A4;
			mKeywordColor = 0xFFFF79C6;
			mStringColor = 0xFFF1FA8C;
			mCommentColor = 0xFF6272A4;
			mNumberColor = 0xFFBD93F9;
			mOperatorColor = 0xFFF8F8F2;
			mAnnotationColor = 0xFF50FA7B;
			mClassColor = 0xFF8BE9FD;
			mMethodColor = 0xFF50FA7B;
			mCurrentLineColor = 0xFF44475A;
			mSelectionColor = 0xFF44475A;
			mGutterBackgroundColor = 0xFF282A36;
			mGutterSeparatorColor = 0xFF44475A;
			mCursorColor = 0xFFF8F8F2;
			break;
			
			case THEME_SOLARIZED_LIGHT:
			mBackgroundColor = 0xFFFDF6E3;
			mTextColor = 0xFF657B83;
			mLineNumberColor = 0xFF93A1A1;
			mKeywordColor = 0xFF859900;
			mStringColor = 0xFF2AA198;
			mCommentColor = 0xFF93A1A1;
			mNumberColor = 0xFFD33682;
			mOperatorColor = 0xFF657B83;
			mAnnotationColor = 0xFFCB4B16;
			mClassColor = 0xFF268BD2;
			mMethodColor = 0xFF268BD2;
			mCurrentLineColor = 0xFFEEE8D5;
			mSelectionColor = 0xFFEEE8D5;
			mGutterBackgroundColor = 0xFFEEE8D5;
			mGutterSeparatorColor = 0xFFDED8C5;
			mCursorColor = 0xFF657B83;
			break;
			
			case THEME_SOLARIZED_DARK:
			mBackgroundColor = 0xFF002B36;
			mTextColor = 0xFF839496;
			mLineNumberColor = 0xFF586E75;
			mKeywordColor = 0xFF859900;
			mStringColor = 0xFF2AA198;
			mCommentColor = 0xFF586E75;
			mNumberColor = 0xFFD33682;
			mOperatorColor = 0xFF839496;
			mAnnotationColor = 0xFFCB4B16;
			mClassColor = 0xFF268BD2;
			mMethodColor = 0xFF268BD2;
			mCurrentLineColor = 0xFF073642;
			mSelectionColor = 0xFF073642;
			mGutterBackgroundColor = 0xFF073642;
			mGutterSeparatorColor = 0xFF083F4D;
			mCursorColor = 0xFF839496;
			break;
			
			case THEME_NORD:
			mBackgroundColor = 0xFF2E3440;
			mTextColor = 0xFFD8DEE9;
			mLineNumberColor = 0xFF4C566A;
			mKeywordColor = 0xFF81A1C1;
			mStringColor = 0xFFA3BE8C;
			mCommentColor = 0xFF616E88;
			mNumberColor = 0xFFB48EAD;
			mOperatorColor = 0xFF81A1C1;
			mAnnotationColor = 0xFF8FBCBB;
			mClassColor = 0xFF8FBCBB;
			mMethodColor = 0xFF88C0D0;
			mCurrentLineColor = 0xFF3B4252;
			mSelectionColor = 0xFF434C5E;
			mGutterBackgroundColor = 0xFF3B4252;
			mGutterSeparatorColor = 0xFF434C5E;
			mCursorColor = 0xFFD8DEE9;
			break;
			
			case THEME_NIGHT_OWL:
			mBackgroundColor = 0xFF011627;
			mTextColor = 0xFFD6DEEB;
			mLineNumberColor = 0xFF4B6479;
			mKeywordColor = 0xFFC792EA;
			mStringColor = 0xFFECC48D;
			mCommentColor = 0xFF637777;
			mNumberColor = 0xFFF78C6C;
			mOperatorColor = 0xFF7FDBCA;
			mAnnotationColor = 0xFFFFCB8B;
			mClassColor = 0xFFFFCB8B;
			mMethodColor = 0xFF82AAFF;
			mCurrentLineColor = 0xFF0E293F;
			mSelectionColor = 0xFF1D3B53;
			mGutterBackgroundColor = 0xFF011627;
			mGutterSeparatorColor = 0xFF1D3B53;
			mCursorColor = 0xFFD6DEEB;
			break;
			
			case THEME_MATERIAL:
			mBackgroundColor = 0xFF263238;
			mTextColor = 0xFFEEFFFF;
			mLineNumberColor = 0xFF546E7A;
			mKeywordColor = 0xFFC792EA;
			mStringColor = 0xFFC3E88D;
			mCommentColor = 0xFF546E7A;
			mNumberColor = 0xFFF78C6C;
			mOperatorColor = 0xFF89DDFF;
			mAnnotationColor = 0xFFFFCB6B;
			mClassColor = 0xFFFFCB6B;
			mMethodColor = 0xFF82AAFF;
			mCurrentLineColor = 0xFF2C3B41;
			mSelectionColor = 0xFF314549;
			mGutterBackgroundColor = 0xFF263238;
			mGutterSeparatorColor = 0xFF314549;
			mCursorColor = 0xFFEEFFFF;
			break;
			
			case THEME_LIGHT:
			default:
			mBackgroundColor = 0xFFFFFFFF;
			mTextColor = 0xFF000000;
			mLineNumberColor = 0xFFAAAAAA;
			mKeywordColor = 0xFF0000FF;
			mStringColor = 0xFF008000;
			mCommentColor = 0xFF808080;
			mNumberColor = 0xFFFF0000;
			mOperatorColor = 0xFF000000;
			mAnnotationColor = 0xFF808000;
			mClassColor = 0xFF0000FF;
			mMethodColor = 0xFF800080;
			mCurrentLineColor = 0xFFF5F5F5;
			mSelectionColor = 0xFFADD6FF;
			mGutterBackgroundColor = 0xFFF5F5F5;
			mGutterSeparatorColor = 0xFFE5E5E5;
			mCursorColor = 0xFF000000;
			break;
		}
		
		// Apply colors
		setBackgroundColor(mBackgroundColor);
		setTextColor(mTextColor);
		setHighlightColor(mSelectionColor);
		
		// Update line number paint if initialized
		if (mLineNumberPaint != null) {
			mLineNumberPaint.setColor(mLineNumberColor);
		}
		
		// Force redraw
		invalidate();
	}
	
	private void initPatterns() {
		mPatterns = new HashMap<>();
		
		if ("java".equals(mLanguage)) {
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null)\\b"));
			
			// Strings
			mPatterns.put("string", Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"));
			
			// Comments
			mPatterns.put("comment", Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/"));
			
			// Numbers
			mPatterns.put("number", Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdD]?\\b"));
			
			// Annotations
			mPatterns.put("annotation", Pattern.compile("@[\\w.]+"));
			
			// Classes
			mPatterns.put("class", Pattern.compile("\\b([A-Z][\\w]*)\\b"));
			
			// Methods
			mPatterns.put("method", Pattern.compile("\\b([a-z][\\w]*)\\s*\\("));
			
			// Operators
			mPatterns.put("operator", Pattern.compile("[+\\-*/%&|^!~=<>?:]+"));
		} else if ("kotlin".equals(mLanguage)) {
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|false|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|true|try|typealias|val|var|vararg|when|where|while)\\b"));
			
			// Strings
			mPatterns.put("string", Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"));
			
			// Comments
			mPatterns.put("comment", Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/"));
			
			// Numbers
			mPatterns.put("number", Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDL]?\\b"));
			
			// Annotations
			mPatterns.put("annotation", Pattern.compile("@[\\w.]+"));
			
			// Classes
			mPatterns.put("class", Pattern.compile("\\b([A-Z][\\w]*)\\b"));
			
			// Methods
			mPatterns.put("method", Pattern.compile("\\b([a-z][\\w]*)\\s*\\("));
			
			// Operators
			mPatterns.put("operator", Pattern.compile("[+\\-*/%&|^!~=<>?:]+"));
		} else if ("python".equals(mLanguage)) {
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield)\\b"));
			
			// Strings
			mPatterns.put("string", Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"));
			
			// Comments
			mPatterns.put("comment", Pattern.compile("#.*"));
			
			// Numbers
			mPatterns.put("number", Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?j?\\b"));
			
			// Decorators
			mPatterns.put("annotation", Pattern.compile("@[\\w.]+"));
			
			// Classes
			mPatterns.put("class", Pattern.compile("\\bclass\\s+([A-Za-z_][\\w]*)"));
			
			// Methods
			mPatterns.put("method", Pattern.compile("\\bdef\\s+([A-Za-z_][\\w]*)"));
			
			// Operators
			mPatterns.put("operator", Pattern.compile("[+\\-*/%&|^!~=<>@]+"));
		} else if ("javascript".equals(mLanguage) || "js".equals(mLanguage)) {
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|of|return|super|switch|this|throw|try|typeof|var|void|while|with|yield)\\b"));
			
			// Strings
			mPatterns.put("string", Pattern.compile("`[\\s\\S]*?`|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"));
			
			// Comments
			mPatterns.put("comment", Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/"));
			
			// Numbers
			mPatterns.put("number", Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b"));
			
			// Classes
			mPatterns.put("class", Pattern.compile("\\bclass\\s+([A-Za-z_$][\\w$]*)"));
			
			// Methods
			mPatterns.put("method", Pattern.compile("\\b([A-Za-z_$][\\w$]*)\\s*\\("));
			
			// Operators
			mPatterns.put("operator", Pattern.compile("[+\\-*/%&|^!~=<>?:]+"));
		} else if ("typescript".equals(mLanguage) || "ts".equals(mLanguage)) {
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(abstract)\\b"));
			
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(abstract|any|as|async|await|boolean|break|case|catch|class|const|constructor|continue|debugger|declare|default|delete|do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in|infer|instanceof|interface|is|keyof|let|module|namespace|never|new|null|number|object|of|package|private|protected|public|readonly|require|return|set|static|string|super|switch|symbol|this|throw|try|type|typeof|undefined|unique|unknown|var|void|while|with|yield)\\b"));
			
			// Strings
			mPatterns.put("string", Pattern.compile("`[\\s\\S]*?`|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"));
			
			// Comments
			mPatterns.put("comment", Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/"));
			
			// Numbers
			mPatterns.put("number", Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b"));
			
			// Decorators
			mPatterns.put("annotation", Pattern.compile("@[\\w.]+"));
			
			// Classes
			mPatterns.put("class", Pattern.compile("\\bclass\\s+([A-Za-z_$][\\w$]*)"));
			
			// Methods
			mPatterns.put("method", Pattern.compile("\\b([A-Za-z_$][\\w$]*)\\s*\\("));
			
			// Operators
			mPatterns.put("operator", Pattern.compile("[+\\-*/%&|^!~=<>?:]+"));
		} else if ("csharp".equals(mLanguage) || "cs".equals(mLanguage)) {
			// Keywords
			mPatterns.put("keyword", Pattern.compile("\\b(abstract|as|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|do|double|else|enum|event|explicit|extern|false|finally|fixed|float|for|foreach|goto|if|implicit|in|int|interface|internal|is|lock|long|namespace|new|null|object|operator|out|override|params|private|protected|public|readonly|ref|return|sbyte|sealed|short|sizeof|stackalloc|static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|virtual|void|volatile|while)\\b"));
			
			// Strings
			mPatterns.put("string", Pattern.compile("@\"[^\"]*(?:\"\"[^\"]*)*\"|\"([^\"\\\\]|\\\\.)*\""));
			
			// Comments
			mPatterns.put("comment", Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/"));
			
			// Numbers
			mPatterns.put("number", Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDmM]?\\b"));
			
			// Attributes
			mPatterns.put("annotation", Pattern.compile("\\[\\s*([A-Za-z_][\\w.]*)\\s*\\]"));
			
			// Classes
			mPatterns.put("class", Pattern.compile("\\bclass\\s+([A-Za-z_][\\w]*)"));
			
			// Methods
			mPatterns.put("method", Pattern.compile("\\b([A-Za-z_][\\w]*)\\s*\\("));
			
			// Operators
			mPatterns.put("operator", Pattern.compile("[+\\-*/%&|^!~=<>?:]+"));
		}
		// Add more languages as needed
	}
	
	/**
	* Sets the code to be displayed.
	* 
	* @param code The code string
	*/
	public void setCode(String code) {
		mFullCode = code;
		
		if (mTypingAnimationEnabled) {
			// Reset typing animation
			mTypingPosition = 0;
			startTypingAnimation();
		} else {
			setText(code);
			
			// Apply syntax highlighting
			if (mEnableHighlighting) {
				highlightSyntax();
			}
			
			// Update line starts for line numbers
			updateLineStarts();
			
			// Update foldable lines
			if (mEnableFolding) {
				updateFoldableLines();
			}
		}
	}
	
	/**
	* Sets the programming language for syntax highlighting.
	* 
	* @param language The language identifier (e.g., "java", "python")
	*/
	public void setLanguage(String language) {
		mLanguage = language;
		initPatterns();
		
		// Apply syntax highlighting
		if (mEnableHighlighting) {
			highlightSyntax();
		}
		
		// Update foldable lines
		if (mEnableFolding) {
			updateFoldableLines();
		}
	}
	
	/**
	* Sets the theme for the code viewer.
	* 
	* @param theme One of the THEME_* constants
	*/
	public void setTheme(int theme) {
		mTheme = theme;
		applyTheme(theme);
	}
	
	/**
	* Sets whether to show line numbers.
	* 
	* @param show True to show line numbers, false to hide
	*/
	public void setShowLineNumbers(boolean show) {
		mShowLineNumbers = show;
		updatePadding();
		invalidate();
	}
	
	/**
	* Sets whether line numbers bar should be fixed when scrolling horizontally.
	* 
	* @param fixed True to fix line numbers, false to scroll with text
	*/
	public void setFixedLineNumbers(boolean fixed) {
		mFixedLineNumbers = fixed;
		invalidate();
	}
	
	/**
	* Sets whether to enable syntax highlighting.
	* 
	* @param enable True to enable syntax highlighting, false to disable
	*/
	public void setEnableHighlighting(boolean enable) {
		mEnableHighlighting = enable;
		if (enable) {
			highlightSyntax();
		} else {
			// Remove all spans
			if (getText() instanceof Spannable) {
				Spannable spannable = (Spannable) getText();
				Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
				for (Object span : spans) {
					if (span instanceof ForegroundColorSpan) {
						spannable.removeSpan(span);
					}
				}
			}
		}
		invalidate();
	}
	
	/**
	* Sets whether to enable line highlighting.
	* 
	* @param enable True to enable line highlighting, false to disable
	*/
	public void setEnableLineHighlighting(boolean enable) {
		mEnableLineHighlighting = enable;
		invalidate();
	}
	
	/**
	* Sets whether to enable code folding.
	* 
	* @param enable True to enable code folding, false to disable
	*/
	public void setEnableFolding(boolean enable) {
		mEnableFolding = enable;
		if (enable) {
			updateFoldableLines();
		} else {
			mFoldableLines.clear();
			mFoldedLines.clear();
		}
		invalidate();
	}
	
	/**
	* Sets whether to enable typing animation.
	* 
	* @param enable True to enable typing animation, false to disable
	*/
	public void setTypingAnimationEnabled(boolean enable) {
		mTypingAnimationEnabled = enable;
		if (enable && mFullCode.length() > 0 && getText().length() == 0) {
			startTypingAnimation();
		}
	}
	
	/**
	* Sets the typing animation speed.
	* 
	* @param speed One of the TYPING_SPEED_* constants
	*/
	public void setTypingSpeed(int speed) {
		mTypingSpeed = speed;
	}
	
	/**
	* Sets whether the code viewer is editable.
	* 
	* @param editable True to make editable, false to make read-only
	*/
	public void setEditable(boolean editable) {
		mIsEditable = editable;
		setFocusable(editable);
		setFocusableInTouchMode(editable);
	}
	
	/**
	* Sets whether to enable auto-indentation.
	* 
	* @param enable True to enable auto-indentation, false to disable
	*/
	public void setAutoIndent(boolean enable) {
		mAutoIndent = enable;
	}
	
	/**
	* Searches for a string in the code.
	* 
	* @param query The search query
	* @return The number of matches found
	*/
	public int search(String query) {
		mSearchQuery = query;
		mSearchResults.clear();
		mCurrentSearchResult = -1;
		
		if (query == null || query.isEmpty()) {
			invalidate();
			return 0;
		}
		
		String text = getText().toString();
		int index = text.indexOf(query);
		while (index >= 0) {
			mSearchResults.add(index);
			index = text.indexOf(query, index + 1);
		}
		
		if (!mSearchResults.isEmpty()) {
			mCurrentSearchResult = 0;
			highlightCurrentSearchResult();
		}
		
		invalidate();
		return mSearchResults.size();
	}
	
	/**
	* Navigates to the next search result.
	* 
	* @return True if navigation was successful, false if no more results
	*/
	public boolean findNext() {
		if (mSearchResults.isEmpty() || mCurrentSearchResult == -1) {
			return false;
		}
		
		mCurrentSearchResult = (mCurrentSearchResult + 1) % mSearchResults.size();
		highlightCurrentSearchResult();
		return true;
	}
	
	/**
	* Navigates to the previous search result.
	* 
	* @return True if navigation was successful, false if no more results
	*/
	public boolean findPrevious() {
		if (mSearchResults.isEmpty() || mCurrentSearchResult == -1) {
			return false;
		}
		
		mCurrentSearchResult = (mCurrentSearchResult - 1 + mSearchResults.size()) % mSearchResults.size();
		highlightCurrentSearchResult();
		return true;
	}
	
	/**
	* Clears the search results.
	*/
	public void clearSearch() {
		mSearchQuery = "";
		mSearchResults.clear();
		mCurrentSearchResult = -1;
		
		// Remove search highlight spans
		if (getText() instanceof Spannable) {
			Spannable spannable = (Spannable) getText();
			BackgroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
			for (BackgroundColorSpan span : spans) {
				if (span == mSearchHighlightSpan) {
					spannable.removeSpan(span);
				}
			}
		}
		
		invalidate();
	}
	
	private void highlightCurrentSearchResult() {
		if (mCurrentSearchResult < 0 || mCurrentSearchResult >= mSearchResults.size()) {
			return;
		}
		
		int start = mSearchResults.get(mCurrentSearchResult);
		int end = start + mSearchQuery.length();
		
		// Remove previous highlight
		if (getText() instanceof Spannable) {
			Spannable spannable = (Spannable) getText();
			spannable.removeSpan(mSearchHighlightSpan);
			
			// Add new highlight
			spannable.setSpan(mSearchHighlightSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			// Scroll to the result
			Layout layout = getLayout();
			if (layout != null) {
				int line = layout.getLineForOffset(start);
				int y = layout.getLineTop(line);
				
				// If we have a parent ScrollView, scroll to the result
				if (mParentScrollView != null) {
					mParentScrollView.smoothScrollTo(0, y);
				} else {
					// Otherwise use built-in scrolling
					scrollTo(0, y);
				}
			}
		}
	}
	
	private void startTypingAnimation() {
		// Stop any existing animation
		if (mTypingRunnable != null) {
			mTypingHandler.removeCallbacks(mTypingRunnable);
		}
		
		// Reset text
		setText("");
		mTypingPosition = 0;
		
		// Start animation
		mTypingRunnable = new Runnable() {
			@Override
			public void run() {
				if (mTypingPosition < mFullCode.length()) {
					// Add next character
					mTypingPosition++;
					setText(mFullCode.substring(0, mTypingPosition));
					
					// Schedule next character
					mTypingHandler.postDelayed(this, mTypingSpeed);
				} else {
					// Animation complete
					if (mEnableHighlighting) {
						highlightSyntax();
					}
				}
			}
		};
		
		mTypingHandler.post(mTypingRunnable);
	}
	
	private void setupCursorAnimation() {
		mCursorAnimator = ValueAnimator.ofFloat(0f, 1f);
		mCursorAnimator.setDuration(500);
		mCursorAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mCursorAnimator.setRepeatMode(ValueAnimator.REVERSE);
		mCursorAnimator.setInterpolator(new DecelerateInterpolator());
		mCursorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				mShowCursor = (float) animation.getAnimatedValue() > 0.5f;
				if (mTypingAnimationEnabled && mTypingPosition < mFullCode.length()) {
					invalidate();
				}
			}
		});
		mCursorAnimator.start();
	}
	
	private Runnable getHighlightRunnable() {
		if (mHighlightRunnable == null) {
			mHighlightRunnable = new Runnable() {
				@Override
				public void run() {
					highlightSyntax();
					mHighlightingScheduled = false;
				}
			};
		}
		return mHighlightRunnable;
	}
	
	private void highlightSyntax() {
		if (!mEnableHighlighting || mPatterns.isEmpty()) {
			return;
		}
		
		CharSequence text = getText();
		if (!(text instanceof Spannable)) {
			return;
		}
		
		Spannable spannable = (Spannable) text;
		String code = text.toString();
		
		// Remove existing color spans
		ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);
		for (ForegroundColorSpan span : spans) {
			spannable.removeSpan(span);
		}
		
		// Apply syntax highlighting
		for (Map.Entry<String, Pattern> entry : mPatterns.entrySet()) {
			String type = entry.getKey();
			Pattern pattern = entry.getValue();
			Matcher matcher = pattern.matcher(code);
			
			int color;
			switch (type) {
				case "keyword":
				color = mKeywordColor;
				break;
				case "string":
				color = mStringColor;
				break;
				case "comment":
				color = mCommentColor;
				break;
				case "number":
				color = mNumberColor;
				break;
				case "annotation":
				color = mAnnotationColor;
				break;
				case "class":
				color = mClassColor;
				break;
				case "method":
				color = mMethodColor;
				break;
				case "operator":
				color = mOperatorColor;
				break;
				default:
				color = mTextColor;
				break;
			}
			
			while (matcher.find()) {
				spannable.setSpan(
				new ForegroundColorSpan(color),
				matcher.start(),
				matcher.end(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				);
			}
		}
	}
	
	private void updateLineStarts() {
		mLineStarts.clear();
		String text = getText().toString();
		mLineStarts.add(0); // First line always starts at 0
		
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				mLineStarts.add(i + 1);
			}
		}
		
		// Update line number width
		String maxLineNumber = String.valueOf(mLineStarts.size());
		mLineNumberWidth = (int) mLineNumberPaint.measureText(maxLineNumber) + mLineNumberPadding * 2;
		
		// Update padding
		updatePadding();
	}
	
	private void updateFoldableLines() {
		mFoldableLines.clear();
		String text = getText().toString();
		String[] lines = text.split("\n");
		
		// Simple heuristic for foldable lines
		int openBraces = 0;
		Map<Integer, Integer> bracePairs = new HashMap<>();
		Stack<Integer> braceStack = new Stack<>();
		
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.contains("{") && !line.contains("//") && !line.contains("/*")) {
				braceStack.push(i);
				openBraces++;
			}
			if (line.contains("}") && !line.contains("//") && !line.contains("/*")) {
				openBraces--;
				if (!braceStack.isEmpty()) {
					int start = braceStack.pop();
					bracePairs.put(start, i);
				}
			}
		}
		
		// Mark lines as foldable
		for (Map.Entry<Integer, Integer> entry : bracePairs.entrySet()) {
			if (entry.getValue() - entry.getKey() > 1) { // Only if there's something to fold
				mFoldableLines.put(entry.getKey(), false); // Not folded by default
			}
		}
	}
	
	private void updatePadding() {
		if (mShowLineNumbers) {
			// We'll handle the padding for line numbers in onDraw instead of setting actual padding
			// This is to avoid issues with text selection and scrolling
			setPadding(mLineNumberWidth, getPaddingTop(), getPaddingRight(), getPaddingBottom());
		} else {
			setPadding(0, getPaddingTop(), getPaddingRight(), getPaddingBottom());
		}
	}
	
	/**
	* Convert a text offset to a line number
	*/
	private int getLineNumberForOffset(int offset) {
		for (int i = 0; i < mLineStarts.size(); i++) {
			int start = mLineStarts.get(i);
			if (i == mLineStarts.size() - 1 || offset < mLineStarts.get(i + 1)) {
				return i;
			}
		}
		return 0;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// Draw background (parent will handle the main background)
		// super.onDraw() will draw the text
		
		// Save canvas state
		canvas.save();
		
		// Text will be drawn in the normal position by super.onDraw(),
		// but we need to adjust our line number drawing based on scrolling
		
		// Draw line numbers if enabled
		if (mShowLineNumbers) {
			// Draw gutter background - fixed position if enabled
			Paint gutterPaint = new Paint();
			gutterPaint.setColor(mGutterBackgroundColor);
			
			if (mFixedLineNumbers) {
				// Fixed position - adjust for horizontal scroll
				canvas.drawRect(0, 0, mLineNumberWidth, getHeight(), gutterPaint);
				
				// Draw gutter separator
				Paint separatorPaint = new Paint();
				separatorPaint.setColor(mGutterSeparatorColor);
				canvas.drawLine(mLineNumberWidth - 1, 0, mLineNumberWidth - 1, getHeight(), separatorPaint);
			} else {
				// Scroll with text
				canvas.drawRect(getScrollX(), getScrollY(), getScrollX() + mLineNumberWidth, getScrollY() + getHeight(), gutterPaint);
				
				// Draw gutter separator
				Paint separatorPaint = new Paint();
				separatorPaint.setColor(mGutterSeparatorColor);
				canvas.drawLine(getScrollX() + mLineNumberWidth - 1, getScrollY(), getScrollX() + mLineNumberWidth - 1, getScrollY() + getHeight(), separatorPaint);
			}
			
			// Draw line numbers
			Layout layout = getLayout();
			if (layout != null) {
				int firstVisibleLine = getFirstVisibleLine();
				int lastVisibleLine = getLastVisibleLine();
				
				for (int i = firstVisibleLine; i <= lastVisibleLine; i++) {
					if (i >= mLineStarts.size()) break;
					
					int lineStart = mLineStarts.get(i);
					int line = layout.getLineForOffset(lineStart);
					int baseline = layout.getLineBaseline(line);
					int top = layout.getLineTop(line);
					
					// Skip folded lines
					if (mFoldedLines.contains(i)) continue;
					
					// Draw line highlight if this is the selected line
					if (mEnableLineHighlighting && i == mSelectedLine) {
						canvas.drawRect(
						mFixedLineNumbers ? mLineNumberWidth : getScrollX() + mLineNumberWidth,
						top,
						getWidth() + getScrollX(),
						layout.getLineBottom(line),
						mSelectedLinePaint
						);
					}
					
					// Draw line number
					String lineNumber = String.valueOf(i + 1);
					float x;
					if (mFixedLineNumbers) {
						// Fixed position
						x = mLineNumberWidth - mLineNumberPadding - mLineNumberPaint.measureText(lineNumber);
					} else {
						// Scroll with text
						x = getScrollX() + mLineNumberWidth - mLineNumberPadding - mLineNumberPaint.measureText(lineNumber);
					}
					canvas.drawText(lineNumber, x, baseline, mLineNumberPaint);
					
					// Draw folding indicator if this line is foldable
					if (mEnableFolding && mFoldableLines.containsKey(i)) {
						boolean folded = mFoldableLines.get(i);
						float indicatorX;
						if (mFixedLineNumbers) {
							indicatorX = mLineNumberWidth - mFoldingIndicatorSize - 2;
						} else {
							indicatorX = getScrollX() + mLineNumberWidth - mFoldingIndicatorSize - 2;
						}
						float indicatorY = baseline - mLineNumberPaint.getTextSize() / 2;
						
						if (folded) {
							// Draw + symbol
							canvas.drawLine(
							indicatorX - mFoldingIndicatorSize / 2,
							indicatorY,
							indicatorX + mFoldingIndicatorSize / 2,
							indicatorY,
							mFoldingPaint
							);
							canvas.drawLine(
							indicatorX,
							indicatorY - mFoldingIndicatorSize / 2,
							indicatorX,
							indicatorY + mFoldingIndicatorSize / 2,
							mFoldingPaint
							);
						} else {
							// Draw - symbol
							canvas.drawLine(
							indicatorX - mFoldingIndicatorSize / 2,
							indicatorY,
							indicatorX + mFoldingIndicatorSize / 2,
							indicatorY,
							mFoldingPaint
							);
						}
					}
				}
			}
		}
		
		// Draw cursor for typing animation
		if (mTypingAnimationEnabled && mShowCursor && mTypingPosition < mFullCode.length()) {
			Layout layout = getLayout();
			if (layout != null) {
				int line = layout.getLineForOffset(mTypingPosition);
				int x = (int) layout.getPrimaryHorizontal(mTypingPosition);
				int top = layout.getLineTop(line);
				int bottom = layout.getLineBottom(line);
				
				Paint cursorPaint = new Paint();
				cursorPaint.setColor(mCursorColor);
				cursorPaint.setStrokeWidth(mCursorWidth);
				canvas.drawLine(x + getPaddingLeft(), top, x + getPaddingLeft(), bottom, cursorPaint);
			}
		}
		
		// Restore canvas state
		canvas.restore();
		
		// Draw the text
		super.onDraw(canvas);
	}
	
	/**
	* Calculate the first visible line based on current scroll position
	*/
	private int getFirstVisibleLine() {
		int scrollY = mParentScrollView != null ? mParentScrollView.getScrollY() : getScrollY();
		Layout layout = getLayout();
		if (layout == null) return 0;
		
		return layout.getLineForVertical(scrollY);
	}
	
	/**
	* Calculate the last visible line based on current scroll position
	*/
	private int getLastVisibleLine() {
		int height = getHeight();
		int scrollY = mParentScrollView != null ? mParentScrollView.getScrollY() : getScrollY();
		Layout layout = getLayout();
		if (layout == null) return 0;
		
		return layout.getLineForVertical(scrollY + height);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean result = mGestureDetector.onTouchEvent(event);
		
		// Handle folding on click in the gutter area
		if (event.getAction() == MotionEvent.ACTION_UP) {
			float x = event.getX();
			float y = event.getY() + getScrollY(); // Adjust for scrolling
			
			if (mShowLineNumbers && ((mFixedLineNumbers && x <= mLineNumberWidth) || 
			(!mFixedLineNumbers && x <= getScrollX() + mLineNumberWidth))) {
				Layout layout = getLayout();
				if (layout != null) {
					int line = layout.getLineForVertical((int) y);
					int offset = layout.getLineStart(line);
					
					// Find the corresponding line number
					int lineNumber = getLineNumberForOffset(offset);
					
					// Toggle folding if this line is foldable
					if (mEnableFolding && mFoldableLines.containsKey(lineNumber)) {
						boolean folded = mFoldableLines.get(lineNumber);
						mFoldableLines.put(lineNumber, !folded);
						invalidate();
						return true;
					}
					
					// Select this line
					mSelectedLine = lineNumber;
					invalidate();
					return true;
				}
			} else {
				// Handle normal text selection clicks
				// Determine the line clicked and select it
				Layout layout = getLayout();
				if (layout != null) {
					// Adjust y for scrolling
					int line = layout.getLineForVertical((int) (y - getScrollY()));
					int offset = layout.getLineStart(line);
					
					// Find the corresponding line number
					mSelectedLine = getLineNumberForOffset(offset);
					invalidate();
				}
			}
		}
		
		// Allow the parent to handle the touch event for scrolling and selection
		boolean parentResult = super.onTouchEvent(event);
		return result || parentResult;
	}
	
	/**
	* Helper class for gesture detection.
	*/
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return false; // Let TextView handle this
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// Update current line for highlighting
			if (mEnableLineHighlighting) {
				int x = (int) e.getX();
				int y = (int) e.getY();
				
				// If tap is in line number gutter, don't select text
				if (mShowLineNumbers && ((mFixedLineNumbers && x <= mLineNumberWidth) || 
				(!mFixedLineNumbers && x <= getScrollX() + mLineNumberWidth))) {
					return true; // We'll handle this in onTouchEvent
				}
			}
			
			return false; // Let TextView handle selection
		}
	}
	
	/**
	* Helper class to represent a syntax highlighting span.
	*/
	private static class CodeSpan {
		int start;
		int end;
		String type;
		
		CodeSpan(int start, int end, String type) {
			this.start = start;
			this.end = end;
			this.type = type;
		}
	}
}
