use hkdf::Hkdf;
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use x25519_dalek::{PublicKey, StaticSecret};
use zeroize::Zeroize;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct SessionParticipant {
    pub node_id: String,
    pub public_key: [u8; 32],
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct OverlaySessionBootstrap {
    pub session_id: [u8; 32],
    pub route_id: Vec<u8>,
    pub initiator: SessionParticipant,
    pub responder: SessionParticipant,
    pub initiator_nonce: [u8; 32],
    pub responder_nonce: [u8; 32],
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct DerivedSessionMaterial {
    pub transcript_hash: [u8; 32],
    pub control_plane_a_to_b: [u8; 32],
    pub control_plane_b_to_a: [u8; 32],
    pub esp_a_to_b: [u8; 32],
    pub esp_b_to_a: [u8; 32],
}

pub fn derive_session_material(
    bootstrap: &OverlaySessionBootstrap,
    local_secret_bytes: [u8; 32],
    remote_public_bytes: [u8; 32],
) -> DerivedSessionMaterial {
    let mut local_secret = StaticSecret::from(local_secret_bytes);
    let remote_public = PublicKey::from(remote_public_bytes);
    let shared = local_secret.diffie_hellman(&remote_public);
    let transcript_hash = build_transcript_hash(bootstrap);

    let mut salt_input = Vec::with_capacity(32 + bootstrap.route_id.len() + 32 + 32 + 32);
    salt_input.extend_from_slice(&bootstrap.session_id);
    salt_input.extend_from_slice(&bootstrap.route_id);
    salt_input.extend_from_slice(&bootstrap.initiator_nonce);
    salt_input.extend_from_slice(&bootstrap.responder_nonce);
    salt_input.extend_from_slice(&transcript_hash);
    let salt = Sha256::digest(&salt_input);

    let hkdf = Hkdf::<Sha256>::new(Some(&salt), shared.as_bytes());

    let mut control_plane_a_to_b = [0u8; 32];
    let mut control_plane_b_to_a = [0u8; 32];
    let mut esp_a_to_b = [0u8; 32];
    let mut esp_b_to_a = [0u8; 32];

    hkdf.expand(b"aso/android/control-plane/a-to-b", &mut control_plane_a_to_b)
        .expect("valid HKDF output length");
    hkdf.expand(b"aso/android/control-plane/b-to-a", &mut control_plane_b_to_a)
        .expect("valid HKDF output length");
    hkdf.expand(b"aso/android/esp/a-to-b", &mut esp_a_to_b)
        .expect("valid HKDF output length");
    hkdf.expand(b"aso/android/esp/b-to-a", &mut esp_b_to_a)
        .expect("valid HKDF output length");

    local_secret.zeroize();

    DerivedSessionMaterial {
        transcript_hash,
        control_plane_a_to_b,
        control_plane_b_to_a,
        esp_a_to_b,
        esp_b_to_a,
    }
}

fn build_transcript_hash(bootstrap: &OverlaySessionBootstrap) -> [u8; 32] {
    let mut hasher = Sha256::new();
    hasher.update(b"AdaptiveSecureOverlay/Android/SessionBootstrap/v1");
    hasher.update(bootstrap.session_id);
    hasher.update((bootstrap.route_id.len() as u32).to_be_bytes());
    hasher.update(&bootstrap.route_id);
    hasher.update(bootstrap.initiator.node_id.as_bytes());
    hasher.update(bootstrap.initiator.public_key);
    hasher.update(bootstrap.responder.node_id.as_bytes());
    hasher.update(bootstrap.responder.public_key);
    hasher.update(bootstrap.initiator_nonce);
    hasher.update(bootstrap.responder_nonce);
    hasher.finalize().into()
}

#[cfg(test)]
mod tests {
    use super::*;
    use x25519_dalek::PublicKey;

    #[test]
    fn both_sides_derive_the_same_material() {
        let initiator_secret = [7u8; 32];
        let responder_secret = [9u8; 32];
        let initiator_public = PublicKey::from(StaticSecret::from(initiator_secret)).to_bytes();
        let responder_public = PublicKey::from(StaticSecret::from(responder_secret)).to_bytes();

        let bootstrap = OverlaySessionBootstrap {
            session_id: [1u8; 32],
            route_id: b"User13->User2 via X1/X2".to_vec(),
            initiator: SessionParticipant {
                node_id: "Android".to_string(),
                public_key: initiator_public,
            },
            responder: SessionParticipant {
                node_id: "User2".to_string(),
                public_key: responder_public,
            },
            initiator_nonce: [2u8; 32],
            responder_nonce: [3u8; 32],
        };

        let left = derive_session_material(&bootstrap, initiator_secret, responder_public);
        let right = derive_session_material(&bootstrap, responder_secret, initiator_public);

        assert_eq!(left, right);
        assert_ne!(left.control_plane_a_to_b, left.control_plane_b_to_a);
        assert_ne!(left.esp_a_to_b, left.esp_b_to_a);
    }
}

