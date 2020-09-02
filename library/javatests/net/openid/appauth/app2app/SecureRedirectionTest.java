package net.openid.appauth.app2app;

import net.openid.appauth.BuildConfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class SecureRedirectionTest {

    @Test
    public void testMatchHashesTrue0() {
        Set<String> set0 = Stream.of("foo", "bar", "baz", "qux", "corge").collect(Collectors.toCollection(HashSet::new));
        Set<String> set1 = Stream.of("baz", "bar", "foo", "corge", "qux").collect(Collectors.toCollection(HashSet::new));

        assertThat(SecureRedirection.matchHashes(set0, set1)).isTrue();
    }

    @Test
    public void testMatchHashesTrue1() {
        Set<String> set0 = Stream.of("foo").collect(Collectors.toCollection(HashSet::new));
        Set<String> set1 = Stream.of("foo").collect(Collectors.toCollection(HashSet::new));

        assertThat(SecureRedirection.matchHashes(set0, set1)).isTrue();
    }

    @Test
    public void testMatchHashesFalse0() {
        Set<String> set0 = Stream.of("foo", "bar", "baz", "qux", "corge").collect(Collectors.toCollection(HashSet::new));
        Set<String> set1 = Stream.of("baz", "fred", "foo", "corge", "qux").collect(Collectors.toCollection(HashSet::new));

        assertThat(SecureRedirection.matchHashes(set0, set1)).isFalse();
    }

    @Test
    public void testMatchHashesFalse1() {
        Set<String> set0 = Stream.of("foo", "bar", "baz", "qux", "corge").collect(Collectors.toCollection(HashSet::new));
        Set<String> set1 = Stream.of("baz", "foo", "corge", "qux").collect(Collectors.toCollection(HashSet::new));

        assertThat(SecureRedirection.matchHashes(set0, set1)).isFalse();
    }
}
