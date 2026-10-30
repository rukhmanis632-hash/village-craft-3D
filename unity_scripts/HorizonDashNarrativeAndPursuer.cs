// ============================================================================
// HORIZON DASH: WORLDS & LEGENDS — UNITY 6 / URP C# NARRATIVE & PURSUER SUITE
// Contains:
//   1. IntroCinematic.cs          (3s Pedestal Relic Grab + Titan Breakout + Tap-to-Skip)
//   2. PursuerController.cs       (8m Anchor, 2m Stumble Surge, 5s Clean Retreat, Capture Takedown)
//   3. DimensionalPortalMorph.cs  (World Rift Trigger & 4-World Pursuer Morph System)
//   4. NarrativeLoreManager.cs    (Main Menu Comic Lore & Milestone Radio Comms UI/Audio)
// ============================================================================

using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Events;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;
using TMPro;

namespace HorizonDash.NarrativeAndPursuer
{
    // ========================================================================
    // 1. INTRO CUTSCENE & GAME START (IntroCinematic.cs)
    // ========================================================================
    public class IntroCinematic : MonoBehaviour
    {
        [Header("Duration & Timing")]
        [Tooltip("Total length of the intro sequence in seconds.")]
        [SerializeField] private float introDuration = 3.0f;
        [SerializeField] private float relicGrabTimestamp = 0.85f;
        [SerializeField] private float titanBreakoutTimestamp = 1.35f;
        [SerializeField] private float cameraBlendStartTimestamp = 2.10f;

        [Header("Scene References")]
        [SerializeField] private Transform playerTransform;
        [SerializeField] private Animator playerAnimator;
        [SerializeField] private Transform chronoCoreRelic;
        [SerializeField] private Transform ancientPedestal;
        [SerializeField] private ParticleSystem wallBreakDebrisVFX;
        [SerializeField] private ParticleSystem relicAbsorbVFX;
        [SerializeField] private PursuerController pursuerController;

        [Header("Camera Rig (Direct or Cinemachine Blend)")]
        [SerializeField] private Camera mainCamera;
        [SerializeField] private Transform cinematicCamAnchor;   // Low-angle hero shot looking at pedestal + ruin wall
        [SerializeField] private Transform gameplayFollowAnchor; // Standard 3rd-person runner follow camera
        [SerializeField] private float cinematicFOV = 52f;
        [SerializeField] private float gameplayFOV = 64f;

        [Header("Audio")]
        [SerializeField] private AudioSource sfxSource;
        [SerializeField] private AudioClip relicGrabClip;
        [SerializeField] private AudioClip wallShatterClip;
        [SerializeField] private AudioClip titanRoarClip;

        [Header("UI Banner")]
        [SerializeField] private CanvasGroup introBannerGroup;
        [SerializeField] private TextMeshProUGUI introStageLabel;

        [Header("Events")]
        public UnityEvent OnIntroStarted;
        public UnityEvent OnIntroCompleted;

        public bool IsPlaying { get; private set; }

        private Coroutine _introRoutine;
        private Vector3 _initialCoreLocalPos;
        private Vector3 _initialCoreScale;

        private void Awake()
        {
            if (chronoCoreRelic != null)
            {
                _initialCoreLocalPos = chronoCoreRelic.localPosition;
                _initialCoreScale = chronoCoreRelic.localScale;
            }
        }

        private void Update()
        {
            if (!IsPlaying) return;

            // Tap-to-Skip: Touching screen or pressing Space/Mouse0 immediately skips to gameplay
            bool touchSkip = Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Began;
            bool mouseSkip = Input.GetMouseButtonDown(0) || Input.GetKeyDown(KeyCode.Space);

            if (touchSkip || mouseSkip)
            {
                SkipIntroImmediately();
            }
        }

        public void PlayIntroSequence()
        {
            if (_introRoutine != null) StopCoroutine(_introRoutine);
            _introRoutine = StartCoroutine(IntroSequenceCoroutine());
        }

        public void SkipIntroImmediately()
        {
            if (!IsPlaying) return;
            if (_introRoutine != null) StopCoroutine(_introRoutine);
            CompleteIntroState();
        }

        private IEnumerator IntroSequenceCoroutine()
        {
            IsPlaying = true;
            OnIntroStarted?.Invoke();

            if (introBannerGroup != null) introBannerGroup.alpha = 1f;
            if (ancientPedestal != null) ancientPedestal.gameObject.SetActive(true);
            if (chronoCoreRelic != null)
            {
                chronoCoreRelic.gameObject.SetActive(true);
                chronoCoreRelic.localPosition = _initialCoreLocalPos;
                chronoCoreRelic.localScale = _initialCoreScale;
            }

            // Snap camera to low-angle pedestal hero shot
            if (mainCamera != null && cinematicCamAnchor != null)
            {
                mainCamera.transform.SetPositionAndRotation(cinematicCamAnchor.position, cinematicCamAnchor.rotation);
                mainCamera.fieldOfView = cinematicFOV;
            }

            playerAnimator?.SetTrigger("GrabRelic");

            bool relicGrabbed = false;
            bool titanSpawned = false;
            float elapsed = 0f;

            while (elapsed < introDuration)
            {
                elapsed += Time.deltaTime;
                float normalizedTime = Mathf.Clamp01(elapsed / introDuration);

                // Hover and spin the Chrono Core before grab
                if (chronoCoreRelic != null && !relicGrabbed)
                {
                    chronoCoreRelic.Rotate(Vector3.up, 220f * Time.deltaTime, Space.Self);
                    chronoCoreRelic.localPosition = _initialCoreLocalPos + Vector3.up * (Mathf.Sin(elapsed * 6f) * 0.15f);
                }

                // Stage 1: Player snatches the Chrono Core (0.85s)
                if (!relicGrabbed && elapsed >= relicGrabTimestamp)
                {
                    relicGrabbed = true;
                    if (introStageLabel != null)
                        introStageLabel.text = "CHRONO CORE ACQUIRED — DIMENSIONAL SEAL BROKEN!";

                    if (relicAbsorbVFX != null) relicAbsorbVFX.Play();
                    if (sfxSource != null && relicGrabClip != null) sfxSource.PlayOneShot(relicGrabClip);

                    if (chronoCoreRelic != null)
                        StartCoroutine(AnimateRelicIntoPlayerRoutine());
                }

                // Stage 2: Chrono Titan shatters ancient wall with screen-shaking roar (1.35s)
                if (!titanSpawned && elapsed >= titanBreakoutTimestamp)
                {
                    titanSpawned = true;
                    if (introStageLabel != null)
                        introStageLabel.text = "WARNING: THE CHRONO TITAN AWAKENS! RUN!";

                    if (wallBreakDebrisVFX != null) wallBreakDebrisVFX.Play();
                    if (sfxSource != null && wallShatterClip != null) sfxSource.PlayOneShot(wallShatterClip);
                    if (sfxSource != null && titanRoarClip != null) sfxSource.PlayOneShot(titanRoarClip);

                    pursuerController?.TriggerIntroBreakout(shakeIntensity: 0.65f, shakeDuration: 0.75f);
                }

                // Stage 3: Smooth camera transition from Cinematic Angle -> 3rd-Person Follow Cam (2.10s..3.00s)
                if (mainCamera != null && cinematicCamAnchor != null && gameplayFollowAnchor != null)
                {
                    if (elapsed >= cameraBlendStartTimestamp)
                    {
                        float blendT = Mathf.Clamp01((elapsed - cameraBlendStartTimestamp) / (introDuration - cameraBlendStartTimestamp));
                        // SmoothStep for cinematic ease-in-out
                        float smoothT = blendT * blendT * (3f - 2f * blendT);

                        mainCamera.transform.position = Vector3.Lerp(cinematicCamAnchor.position, gameplayFollowAnchor.position, smoothT);
                        mainCamera.transform.rotation = Quaternion.Slerp(cinematicCamAnchor.rotation, gameplayFollowAnchor.rotation, smoothT);
                        mainCamera.fieldOfView = Mathf.Lerp(cinematicFOV, gameplayFOV, smoothT);
                    }
                    else if (titanSpawned)
                    {
                        // Subtle camera recoil shake during Titan roar
                        Vector3 shakeOffset = UnityEngine.Random.insideUnitSphere * 0.14f;
                        mainCamera.transform.position = cinematicCamAnchor.position + shakeOffset;
                    }
                }

                yield return null;
            }

            CompleteIntroState();
        }

        private IEnumerator AnimateRelicIntoPlayerRoutine()
        {
            float t = 0f;
            Vector3 startPos = chronoCoreRelic.position;
            while (t < 0.35f && chronoCoreRelic != null && playerTransform != null)
            {
                t += Time.deltaTime;
                float k = t / 0.35f;
                chronoCoreRelic.position = Vector3.Lerp(startPos, playerTransform.position + Vector3.up * 1.2f, k);
                chronoCoreRelic.localScale = Vector3.Lerp(_initialCoreScale, Vector3.zero, k);
                yield return null;
            }
            if (chronoCoreRelic != null) chronoCoreRelic.gameObject.SetActive(false);
        }

        private void CompleteIntroState()
        {
            IsPlaying = false;
            if (introBannerGroup != null) introBannerGroup.alpha = 0f;
            if (chronoCoreRelic != null) chronoCoreRelic.gameObject.SetActive(false);

            if (mainCamera != null && gameplayFollowAnchor != null)
            {
                mainCamera.transform.SetPositionAndRotation(gameplayFollowAnchor.position, gameplayFollowAnchor.rotation);
                mainCamera.fieldOfView = gameplayFOV;
            }

            playerAnimator?.SetTrigger("StartRun");
            pursuerController?.EnterStandardPursuit();
            OnIntroCompleted?.Invoke();
        }
    }

    // ========================================================================
    // 2. DYNAMIC PURSUER SYSTEM & AI STATE MACHINE (PursuerController.cs)
    // ========================================================================
    public enum PursuerState
    {
        IntroBreakout,    // Shattering temple wall during 3s intro
        DistantPursuit,   // Standard anchor at 8.0m behind player
        DangerClose,      // Surged to 2.0m after player stumble (high tension)
        Retreating,       // Clean run for 5.0s -> retreating smoothly from 2.0m back to 8.0m
        PortalLeap,       // Leaping into Dimensional Rift alongside player
        CaptureTakedown   // Second stumble while close -> lethal capture animation
    }

    public class PursuerController : MonoBehaviour
    {
        [Header("Target & Anchor Distances")]
        [SerializeField] private Transform playerTransform;
        [SerializeField] private float standardAnchorDistance = 8.0f;
        [SerializeField] private float dangerCloseDistance = 2.0f;
        [SerializeField] private float lethalCaptureThreshold = 3.6f;
        [SerializeField] private float cleanRunRecoveryDuration = 5.0f;

        [Header("Movement Smoothing")]
        [SerializeField] private float surgeApproachSpeed = 14.0f;
        [SerializeField] private float retreatSpeed = 2.2f;
        [SerializeField] private float lateralFollowSpeed = 9.5f;

        [Header("URP Post-Processing & Camera Feedback")]
        [SerializeField] private Camera gameplayCamera;
        [SerializeField] private Volume urpGlobalVolume;
        [SerializeField] private float baseFOV = 64f;
        [SerializeField] private float dangerZoomFOV = 54f; // Dynamic FOV zoom-in on stumble surge
        [SerializeField] private float dangerVignetteIntensity = 0.48f;
        [SerializeField] private float dangerChromaticIntensity = 0.75f;

        [Header("Animation & Audio")]
        [SerializeField] private Animator pursuerAnimator;
        [SerializeField] private AudioSource roarAudioSource;
        [SerializeField] private AudioClip surgeRoarClip;
        [SerializeField] private AudioClip takedownRoarClip;
        [SerializeField] private AudioClip heavyFootstepClip;

        [Header("Events")]
        public UnityEvent OnPlayerStumbleSurge;
        public UnityEvent OnPlayerCaptured;

        public PursuerState CurrentState { get; private set; } = PursuerState.DistantPursuit;
        public float CurrentDistanceBehindPlayer { get; private set; } = 8.0f;
        public float CleanRunTimer { get; private set; } = 0f;

        private float _targetDistance = 8.0f;
        private float _shakeTimer = 0f;
        private float _shakeAmplitude = 0f;
        private float _fovPunchOffset = 0f;

        private Vignette _vignette;
        private ChromaticAberration _chromaticAberration;

        private void Awake()
        {
            CurrentDistanceBehindPlayer = standardAnchorDistance;
            _targetDistance = standardAnchorDistance;

            if (urpGlobalVolume != null && urpGlobalVolume.profile != null)
            {
                urpGlobalVolume.profile.TryGet(out _vignette);
                urpGlobalVolume.profile.TryGet(out _chromaticAberration);
            }
        }

        private void Update()
        {
            if (playerTransform == null) return;

            UpdateStateMachine(Time.deltaTime);
            UpdatePursuerTransform(Time.deltaTime);
            UpdateCameraAndURPEffects(Time.deltaTime);
        }

        private void UpdateStateMachine(float dt)
        {
            switch (CurrentState)
            {
                case PursuerState.DistantPursuit:
                    _targetDistance = standardAnchorDistance;
                    CurrentDistanceBehindPlayer = Mathf.MoveTowards(
                        CurrentDistanceBehindPlayer, _targetDistance, retreatSpeed * dt);
                    break;

                case PursuerState.DangerClose:
                    _targetDistance = dangerCloseDistance;
                    CurrentDistanceBehindPlayer = Mathf.MoveTowards(
                        CurrentDistanceBehindPlayer, _targetDistance, surgeApproachSpeed * dt);

                    // Count clean running time; after 5 seconds without stumbling, begin retreating
                    CleanRunTimer += dt;
                    if (CleanRunTimer >= cleanRunRecoveryDuration)
                    {
                        CurrentState = PursuerState.Retreating;
                        pursuerAnimator?.SetBool("IsEnragedClose", false);
                    }
                    break;

                case PursuerState.Retreating:
                    _targetDistance = standardAnchorDistance;
                    CurrentDistanceBehindPlayer = Mathf.MoveTowards(
                        CurrentDistanceBehindPlayer, _targetDistance, retreatSpeed * dt);

                    if (Mathf.Approximately(CurrentDistanceBehindPlayer, standardAnchorDistance))
                    {
                        CurrentState = PursuerState.DistantPursuit;
                        CleanRunTimer = 0f;
                    }
                    break;

                case PursuerState.PortalLeap:
                    // Hold tight behind player while leaping through the dimensional rift
                    CurrentDistanceBehindPlayer = Mathf.MoveTowards(
                        CurrentDistanceBehindPlayer, 3.2f, surgeApproachSpeed * dt);
                    break;

                case PursuerState.CaptureTakedown:
                    // Lock onto player position for capture takedown
                    CurrentDistanceBehindPlayer = Mathf.MoveTowards(
                        CurrentDistanceBehindPlayer, 0.35f, surgeApproachSpeed * 1.5f * dt);
                    break;
            }
        }

        private void UpdatePursuerTransform(float dt)
        {
            Vector3 targetPos = playerTransform.position - playerTransform.forward * CurrentDistanceBehindPlayer;
            Vector3 currentPos = transform.position;

            // Smooth lateral lane tracking + exact Z distance tracking
            float newX = Mathf.Lerp(currentPos.x, targetPos.x, lateralFollowSpeed * dt);
            float newY = Mathf.Lerp(currentPos.y, targetPos.y, lateralFollowSpeed * dt);
            transform.position = new Vector3(newX, newY, targetPos.z);
        }

        private void UpdateCameraAndURPEffects(float dt)
        {
            float proximityDangerFactor = Mathf.InverseLerp(standardAnchorDistance, dangerCloseDistance, CurrentDistanceBehindPlayer);

            // Dynamic FOV zoom-in when monster surges close
            if (gameplayCamera != null)
            {
                _fovPunchOffset = Mathf.MoveTowards(_fovPunchOffset, 0f, 18f * dt);
                float targetFOV = Mathf.Lerp(baseFOV, dangerZoomFOV, proximityDangerFactor * 0.5f) + _fovPunchOffset;
                gameplayCamera.fieldOfView = Mathf.Lerp(gameplayCamera.fieldOfView, targetFOV, 10f * dt);

                if (_shakeTimer > 0f)
                {
                    _shakeTimer -= dt;
                    gameplayCamera.transform.localPosition += UnityEngine.Random.insideUnitSphere * _shakeAmplitude;
                }
            }

            // URP Volume danger feedback (Crimson Vignette + Chromatic Aberration)
            if (_vignette != null)
            {
                _vignette.intensity.value = Mathf.Lerp(0.18f, dangerVignetteIntensity, proximityDangerFactor);
            }
            if (_chromaticAberration != null)
            {
                _chromaticAberration.intensity.value = Mathf.Lerp(0.05f, dangerChromaticIntensity, proximityDangerFactor);
            }
        }

        /// <summary>
        /// Call when the player clips a side barrier, trips on a low hurdle, or stumbles.
        /// </summary>
        public void OnPlayerStumbled()
        {
            if (CurrentState == PursuerState.CaptureTakedown) return;

            // Check if Pursuer is already close -> Trigger Capture/Takedown Death Animation!
            bool isAlreadyClose = CurrentState == PursuerState.DangerClose ||
                                  CurrentDistanceBehindPlayer <= lethalCaptureThreshold;

            if (isAlreadyClose)
            {
                TriggerCaptureTakedown();
                return;
            }

            // First stumble: Monster surges forward from 8m -> 2m with FOV zoom, shake & roar
            CurrentState = PursuerState.DangerClose;
            CleanRunTimer = 0f;
            _targetDistance = dangerCloseDistance;
            _fovPunchOffset = -8.5f; // Instant FOV zoom-in punch
            TriggerCameraShake(amplitude: 0.45f, duration: 0.55f);

            pursuerAnimator?.SetTrigger("SurgeForward");
            pursuerAnimator?.SetBool("IsEnragedClose", true);

            if (roarAudioSource != null && surgeRoarClip != null)
                roarAudioSource.PlayOneShot(surgeRoarClip);

            OnPlayerStumbleSurge?.Invoke();
        }

        public void TriggerCaptureTakedown()
        {
            CurrentState = PursuerState.CaptureTakedown;
            _targetDistance = 0.25f;
            _fovPunchOffset = -12f;
            TriggerCameraShake(amplitude: 0.75f, duration: 0.9f);

            pursuerAnimator?.SetTrigger("CaptureTakedown");
            if (roarAudioSource != null && takedownRoarClip != null)
                roarAudioSource.PlayOneShot(takedownRoarClip);

            OnPlayerCaptured?.Invoke();
        }

        public void TriggerIntroBreakout(float shakeIntensity, float shakeDuration)
        {
            CurrentState = PursuerState.IntroBreakout;
            CurrentDistanceBehindPlayer = 5.5f;
            TriggerCameraShake(shakeIntensity, shakeDuration);
            pursuerAnimator?.SetTrigger("BreakoutRoar");
        }

        public void EnterStandardPursuit()
        {
            CurrentState = PursuerState.DistantPursuit;
            CurrentDistanceBehindPlayer = standardAnchorDistance;
            _targetDistance = standardAnchorDistance;
            CleanRunTimer = 0f;
        }

        public void NotifyPortalLeap()
        {
            CurrentState = PursuerState.PortalLeap;
            pursuerAnimator?.SetTrigger("PortalLeap");
            StartCoroutine(ExitPortalLeapAfterDelay(0.65f));
        }

        private IEnumerator ExitPortalLeapAfterDelay(float delay)
        {
            yield return new WaitForSeconds(delay);
            if (CurrentState == PursuerState.PortalLeap)
            {
                CurrentState = PursuerState.DistantPursuit;
                _targetDistance = standardAnchorDistance;
            }
        }

        public void TriggerCameraShake(float amplitude, float duration)
        {
            _shakeAmplitude = amplitude;
            _shakeTimer = duration;
        }
    }

    // ========================================================================
    // 3. THEMATIC WORLD PORTAL & PURSUER MORPH (DimensionalPortalMorph.cs)
    // ========================================================================
    public enum WorldBiome
    {
        AncientRuins = 0, // Mossy Stone Golem
        CyberCity = 1,    // Heavy Mecha-Drone / Cyber Beast
        LavaCave = 2,     // Molten Magma Colossus
        SnowyPeak = 3     // Frost / Glacier Yeti
    }

    [Serializable]
    public struct PursuerBiomeMorphData
    {
        public WorldBiome biome;
        public string formTitle;
        public GameObject formModelRoot;          // Toggleable mesh/rig hierarchy per biome
        public Material primaryArmorMaterial;     // URP Lit / Emission material override
        public Color coreEmissionColor;
        public ParticleSystem ambientTrailVFX;    // Falling moss rocks, cyber sparks, magma embers, or frost mist
        public AudioClip morphRoarOverride;
    }

    public class DimensionalPortalMorph : MonoBehaviour
    {
        [Header("Pursuer Reference")]
        [SerializeField] private PursuerController pursuerController;
        [SerializeField] private SkinnedMeshRenderer sharedMeshRenderer; // Optional shared mesh material swap
        [SerializeField] private ParticleSystem portalWarpBurstVFX;
        [SerializeField] private AudioSource portalAudioSource;
        [SerializeField] private AudioClip portalEnterSFX;

        [Header("World Morph Definitions (4 Worlds)")]
        [SerializeField] private List<PursuerBiomeMorphData> biomeMorphs = new List<PursuerBiomeMorphData>();

        public WorldBiome CurrentBiome { get; private set; } = WorldBiome.AncientRuins;

        private static readonly int EmissionColorProperty = Shader.PropertyToID("_EmissionColor");

        private void Start()
        {
            ApplyPursuerMorph(WorldBiome.AncientRuins, playEffects: false);
        }

        /// <summary>
        /// Invoked when the player collides with a Dimensional Rift Portal on the track.
        /// </summary>
        public void EnterDimensionalPortal(WorldBiome destinationBiome)
        {
            CurrentBiome = destinationBiome;

            // 1. Make the Pursuer leap into the rift behind the player
            pursuerController?.NotifyPortalLeap();

            // 2. Swap Pursuer model / URP emissive materials to match the new biome
            ApplyPursuerMorph(destinationBiome, playEffects: true);
        }

        public void CycleToNextWorldPortal()
        {
            int nextIndex = ((int)CurrentBiome + 1) % 4;
            EnterDimensionalPortal((WorldBiome)nextIndex);
        }

        public void ApplyPursuerMorph(WorldBiome targetBiome, bool playEffects)
        {
            if (playEffects)
            {
                if (portalWarpBurstVFX != null) portalWarpBurstVFX.Play();
                if (portalAudioSource != null && portalEnterSFX != null)
                    portalAudioSource.PlayOneShot(portalEnterSFX);
            }

            foreach (var morph in biomeMorphs)
            {
                bool isActiveBiome = morph.biome == targetBiome;

                if (morph.formModelRoot != null)
                    morph.formModelRoot.SetActive(isActiveBiome);

                if (morph.ambientTrailVFX != null)
                {
                    if (isActiveBiome) morph.ambientTrailVFX.Play();
                    else morph.ambientTrailVFX.Stop();
                }

                if (isActiveBiome)
                {
                    if (sharedMeshRenderer != null && morph.primaryArmorMaterial != null)
                    {
                        sharedMeshRenderer.material = morph.primaryArmorMaterial;
                        sharedMeshRenderer.material.EnableKeyword("_EMISSION");
                        sharedMeshRenderer.material.SetColor(EmissionColorProperty, morph.coreEmissionColor * 2.8f);
                    }

                    if (playEffects && portalAudioSource != null && morph.morphRoarOverride != null)
                    {
                        portalAudioSource.PlayOneShot(morph.morphRoarOverride);
                    }
                }
            }
        }
    }

    // ========================================================================
    // 4. NARRATIVE UI & LORE INTEGRATION (NarrativeLoreManager.cs)
    // ========================================================================
    [Serializable]
    public struct LoreComicFrame
    {
        public string panelNumber;
        public string title;
        [TextArea(2, 4)] public string caption;
        public Sprite comicIllustration;
        public Color accentColor;
    }

    [Serializable]
    public struct MilestoneRadioLine
    {
        public int distanceMeters;
        public string speakerCallsign;
        [TextArea(1, 3)] public string messageText;
        public AudioClip voiceOrChimeClip;
        public bool triggersDimensionalPortal;
    }

    public class NarrativeLoreManager : MonoBehaviour
    {
        [Header("Main Menu Story / Lore Comic Modal")]
        [SerializeField] private GameObject storyLoreModalRoot;
        [SerializeField] private List<LoreComicFrame> comicFrames = new List<LoreComicFrame>();

        [Header("In-Game Radio Comms HUD")]
        [SerializeField] private CanvasGroup radioCommsCanvasGroup;
        [SerializeField] private TextMeshProUGUI speakerCallsignText;
        [SerializeField] private TextMeshProUGUI transmissionBodyText;
        [SerializeField] private AudioSource radioAudioSource;
        [SerializeField] private AudioClip defaultRadioChimeClip;

        [Header("Distance Milestone Voice Lines")]
        [SerializeField] private List<MilestoneRadioLine> milestoneLines = new List<MilestoneRadioLine>()
        {
            new MilestoneRadioLine
            {
                distanceMeters = 100,
                speakerCallsign = "HQ // ORACLE-7",
                messageText = "Chrono Core signature locked! Don't stumble—the Titan is right behind you!",
                triggersDimensionalPortal = false
            },
            new MilestoneRadioLine
            {
                distanceMeters = 300,
                speakerCallsign = "HQ // ORACLE-7",
                messageText = "Dimensional Rift ahead! Leap through to shift worlds—warning, the Titan adapts its form!",
                triggersDimensionalPortal = true
            },
            new MilestoneRadioLine
            {
                distanceMeters = 600,
                speakerCallsign = "DR. VANCE // ARCHIVE",
                messageText = "Core resonance rising! Keep your flow clean for 5 seconds to force the Pursuer back!",
                triggersDimensionalPortal = false
            },
            new MilestoneRadioLine
            {
                distanceMeters = 1000,
                speakerCallsign = "CHRONO CORE // ALERT",
                messageText = "Chrono Core destabilizing... Portal opening!",
                triggersDimensionalPortal = true
            }
        };

        private readonly HashSet<int> _triggeredMilestones = new HashSet<int>();
        private Coroutine _activePopupRoutine;

        public void OpenStoryLoreModal()
        {
            if (storyLoreModalRoot != null)
                storyLoreModalRoot.SetActive(true);
        }

        public void CloseStoryLoreModal()
        {
            if (storyLoreModalRoot != null)
                storyLoreModalRoot.SetActive(false);
        }

        public void ResetRunMilestones()
        {
            _triggeredMilestones.Clear();
            if (radioCommsCanvasGroup != null)
                radioCommsCanvasGroup.alpha = 0f;
        }

        /// <summary>
        /// Call each frame from the main runner manager with the current run distance in meters.
        /// </summary>
        public void EvaluateDistanceMilestones(float currentDistanceMeters, Action onSpawnDimensionalPortal)
        {
            int distInt = Mathf.FloorToInt(currentDistanceMeters);

            for (int i = 0; i < milestoneLines.Count; i++)
            {
                var line = milestoneLines[i];
                if (distInt >= line.distanceMeters && !_triggeredMilestones.Contains(line.distanceMeters))
                {
                    _triggeredMilestones.Add(line.distanceMeters);
                    TriggerRadioTransmission(line);

                    if (line.triggersDimensionalPortal)
                    {
                        onSpawnDimensionalPortal?.Invoke();
                    }
                }
            }
        }

        public void TriggerRadioTransmission(MilestoneRadioLine line)
        {
            if (speakerCallsignText != null) speakerCallsignText.text = line.speakerCallsign;
            if (transmissionBodyText != null) transmissionBodyText.text = $"\"{line.messageText}\"";

            if (radioAudioSource != null)
            {
                AudioClip clipToPlay = line.voiceOrChimeClip != null ? line.voiceOrChimeClip : defaultRadioChimeClip;
                if (clipToPlay != null) radioAudioSource.PlayOneShot(clipToPlay);
            }

            if (_activePopupRoutine != null) StopCoroutine(_activePopupRoutine);
            _activePopupRoutine = StartCoroutine(ShowRadioBannerRoutine(3.8f));
        }

        private IEnumerator ShowRadioBannerRoutine(float visibleSeconds)
        {
            if (radioCommsCanvasGroup == null) yield break;

            // Fade in
            float t = 0f;
            while (t < 0.2f)
            {
                t += Time.deltaTime;
                radioCommsCanvasGroup.alpha = Mathf.Clamp01(t / 0.2f);
                yield return null;
            }

            yield return new WaitForSeconds(visibleSeconds);

            // Fade out
            t = 0f;
            while (t < 0.35f)
            {
                t += Time.deltaTime;
                radioCommsCanvasGroup.alpha = 1f - Mathf.Clamp01(t / 0.35f);
                yield return null;
            }
            radioCommsCanvasGroup.alpha = 0f;
        }
    }
}
