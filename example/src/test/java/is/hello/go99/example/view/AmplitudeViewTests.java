/*
 * Copyright 2015 Hello Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.go99.example.view;

import org.junit.Test;

import is.hello.go99.example.ExampleTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AmplitudeViewTests extends ExampleTestCase {
    private final AmplitudeView.Colors colors;

    public AmplitudeViewTests() {
        this.colors = new AmplitudeView.Colors(getResources());
    }


    @Test
    public void getColor() {
        assertThat(colors.getColor(0.0f, 0xff), is(equalTo(0xffBBDEFB)));
        assertThat(colors.getColor(0.0f, 0x88), is(equalTo(0x88BBDEFB)));
        assertThat(colors.getColor(0.5f, 0xff), is(equalTo(0xff90CAF9)));
        assertThat(colors.getColor(0.5f, 0x88), is(equalTo(0x8890CAF9)));
        assertThat(colors.getColor(0.8f, 0xff), is(equalTo(0xff64B5F6)));
        assertThat(colors.getColor(0.8f, 0x88), is(equalTo(0x8864B5F6)));
    }

    @Test
    public void getAnimatorColorsIncremental() {
        assertThat(colors.getAnimatorColors(0.0f, 1.0f),
                   is(equalTo(new int[] { 0xffBBDEFB, 0xff90CAF9, 0xff64B5F6} )));
        assertThat(colors.getAnimatorColors(0.0f, 0.5f),
                   is(equalTo(new int[] { 0xffBBDEFB, 0xff90CAF9 })));
    }

    @Test
    public void getAnimatorColorsDecremental() {
        assertThat(colors.getAnimatorColors(1.0f, 0.0f),
                   is(equalTo(new int[] { 0xff64B5F6, 0xff90CAF9, 0xffBBDEFB } )));
        assertThat(colors.getAnimatorColors(0.5f, 0.0f),
                   is(equalTo(new int[] { 0xff90CAF9, 0xffBBDEFB, })));
    }

    @Test
    public void getAnimatorColorsSame() {
        assertThat(colors.getAnimatorColors(1.0f, 1.0f),
                   is(equalTo(new int[] { 0xff64B5F6, 0xff64B5F6} )));
        assertThat(colors.getAnimatorColors(0.5f, 0.5f),
                   is(equalTo(new int[] { 0xff90CAF9, 0xff90CAF9 })));
        assertThat(colors.getAnimatorColors(0.0f, 0.0f),
                   is(equalTo(new int[] { 0xffBBDEFB, 0xffBBDEFB })));
    }
}
