"""Tests for the security helpers."""
import pytest

from app.core.security import (
    create_access_token,
    create_refresh_token,
    decrypt_token,
    encrypt_token,
    get_password_hash,
    verify_password,
    verify_token,
)


def test_password_hash_and_verify():
    hashed = get_password_hash("secret123")
    assert verify_password("secret123", hashed)
    assert not verify_password("wrong", hashed)


def test_access_token_roundtrip():
    token = create_access_token({"sub": "user-uuid-123"})
    payload = verify_token(token)
    assert payload is not None
    assert payload["sub"] == "user-uuid-123"
    assert payload["type"] == "access"


def test_refresh_token_type():
    token = create_refresh_token({"sub": "user-uuid-456"})
    payload = verify_token(token)
    assert payload is not None
    assert payload["type"] == "refresh"


def test_invalid_token_returns_none():
    assert verify_token("not.a.valid.token") is None
    assert verify_token("") is None


def test_encrypt_decrypt_token():
    original = "my-secret-oauth-access-token"
    encrypted = encrypt_token(original)
    assert encrypted != original
    decrypted = decrypt_token(encrypted)
    assert decrypted == original
