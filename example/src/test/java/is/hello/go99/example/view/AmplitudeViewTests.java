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
        assertThat(colors.getColor(0.0f, 0xff), is(equalTo(0xffE3F2FD)));
        assertThat(colors.getColor(0.0f, 0x88), is(equalTo(0x88E3F2FD)));
        assertThat(colors.getColor(0.5f, 0xff), is(equalTo(0xffBBDEFB)));
        assertThat(colors.getColor(0.5f, 0x88), is(equalTo(0x88BBDEFB)));
        assertThat(colors.getColor(0.8f, 0xff), is(equalTo(0xff90CAF9)));
        assertThat(colors.getColor(0.8f, 0x88), is(equalTo(0x8890CAF9)));
    }

    @Test
    public void getAnimatorColorsIncremental() {
        assertThat(colors.getAnimatorColors(0.0f, 1.0f),
                   is(equalTo(new int[] { 0xffE3F2FD, 0xffBBDEFB, 0xff90CAF9 } )));
        assertThat(colors.getAnimatorColors(0.0f, 0.5f),
                   is(equalTo(new int[] { 0xffE3F2FD, 0xffBBDEFB })));
    }

    @Test
    public void getAnimatorColorsDecremental() {
        assertThat(colors.getAnimatorColors(1.0f, 0.0f),
                   is(equalTo(new int[] { 0xff90CAF9, 0xffBBDEFB, 0xffE3F2FD } )));
        assertThat(colors.getAnimatorColors(0.5f, 0.0f),
                   is(equalTo(new int[] { 0xffBBDEFB, 0xffE3F2FD, })));
    }

    @Test
    public void getAnimatorColorsSame() {
        assertThat(colors.getAnimatorColors(1.0f, 1.0f),
                   is(equalTo(new int[] { 0xff90CAF9, 0xff90CAF9 } )));
        assertThat(colors.getAnimatorColors(0.5f, 0.5f),
                   is(equalTo(new int[] { 0xffBBDEFB, 0xffBBDEFB })));
        assertThat(colors.getAnimatorColors(0.0f, 0.0f),
                   is(equalTo(new int[] { 0xffE3F2FD, 0xffE3F2FD })));
    }
}
