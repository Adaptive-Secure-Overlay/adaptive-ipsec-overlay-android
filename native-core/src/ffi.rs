use crate::{derive_session_material, OverlaySessionBootstrap, SessionParticipant};
use jni::objects::JClass;
use jni::sys::{jboolean, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use x25519_dalek::{PublicKey, StaticSecret};

#[unsafe(no_mangle)]
pub extern "system" fn Java_ru_adaptive_overlay_NativeCryptoBridge_nativeVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    match env.new_string("adaptive-native-core/0.1.0-jni") {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_ru_adaptive_overlay_NativeCryptoBridge_nativeSelfTest(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if self_test() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

fn self_test() -> bool {
    let initiator_secret = [7u8; 32];
    let responder_secret = [9u8; 32];
    let initiator_secret_obj = StaticSecret::from(initiator_secret);
    let responder_secret_obj = StaticSecret::from(responder_secret);
    let initiator_public = PublicKey::from(&initiator_secret_obj).to_bytes();
    let responder_public = PublicKey::from(&responder_secret_obj).to_bytes();

    let bootstrap = OverlaySessionBootstrap {
        session_id: [1u8; 32],
        route_id: b"Android->User2 via X1/X2".to_vec(),
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

    left == right
        && left.control_plane_a_to_b != left.control_plane_b_to_a
        && left.esp_a_to_b != left.esp_b_to_a
}
