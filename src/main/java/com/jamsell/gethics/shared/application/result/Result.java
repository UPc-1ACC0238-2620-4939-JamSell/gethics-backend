package com.jamsell.gethics.shared.application.result;

import java.util.Optional;
import java.util.function.Function;

public sealed interface Result<T, E> {

    record Success<T, E>(T value) implements Result<T, E> {
    }

    record Failure<T, E>(E error) implements Result<T, E> {
    }

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T, E> success -> Optional.ofNullable(success.value());
            case Failure<T, E> failure -> Optional.empty();
        };
    }

    default <T2> Result<T2, E> map(Function<T, T2> mapper) {
        return switch (this) {
            case Success<T, E> success -> Result.success(mapper.apply(success.value()));
            case Failure<T, E> failure -> Result.failure(failure.error());
        };
    }
}
