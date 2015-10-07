![anime-android-go-99](imagery/99-grey-header-small.png "anime-android-go-99")

Tools to make building complex animations on Android more pleasant.

# Introduction

Android has pretty decent APIs available for building animations, but they tend to be fragmented, and coordinating more than a couple quickly becomes a burden. anime-android-go-99 provides tools to make it easier to create consistent animations, and act on them as a whole.

# The important parts

## Animator contexts

Animator contexts make it easier to deal with a large number of animations. The main features offered by animator contexts are:

- Transactions: With a transaction, you can easily create multiple animations that follow the same basic timing, and coordinate their execution.
- Fire when idle: Animator context tracks any animations that are running within it, and allow you to schedule additional animations and work to fire when all current work is completed.

## Animator templates

Animator templates provide a lightweight abstraction for ensuring all of your animations use the same duration and timing function. An animator template can be applied to most animation primitives in the Android APIs, and can be applied to all APIs provided by anime-android-go-99.

## Multi-animators

Android's [`ViewPropertyAnimator`](http://developer.android.com/reference/android/view/ViewPropertyAnimator.html) is great, but it does not extend `Animator`, and can be tricky to coordinate a single `View`'s animation from different parts of your application. anime-android-go-99 provides the `MultiAnimator` class as a lightweight wrapper around `ViewPropertyAnimator` that allows you to treat low level view animations the same as any other `Animator`.

# License

	Copyright 2015 Hello, Inc
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	   http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
