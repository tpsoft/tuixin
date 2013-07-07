package com.tpsoft.tuixin.utils;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

/**
 * ���ò�����Ч���� ֻ������Ҫ���õķ����������
 * 
 * PlaySoundPool playSoundPool=new PlaySoundPool(context);
 * playSoundPool.loadSfx(R.raw.fanye, 1);
 * playSoundPool.play(1, 0);
 * 
 */
public class PlaySoundPool {
	private Context context;

	public PlaySoundPool(Context context) {
		this.context = context;
		initSounds();
	}

	// ��Ч������
	int streamVolume;

	// ����SoundPool ����
	private SoundPool soundPool;

	// ����HASH��
	private HashMap<Integer, Integer> soundPoolMap;

	@SuppressLint("UseSparseArrays")
	public void initSounds() {
		// ��ʼ��soundPool ����,��һ�������������ж��ٸ�������ͬʱ����,��2����������������,������������������Ʒ��
		soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC, 100);

		// ��ʼ��HASH��
		soundPoolMap = new HashMap<Integer, Integer>();

		// ��������豸���豸����
		AudioManager mgr = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		streamVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	public void loadSfx(int raw, int id) {
		// ����Դ�е���Ч���ص�ָ����ID(���ŵ�ʱ��Ͷ�Ӧ�����ID���ž�����)
		soundPoolMap.put(id, soundPool.load(context, raw, id));
	}

	public void play(int sound, int uLoop) {
		soundPool.play(soundPoolMap.get(sound), streamVolume, streamVolume, 1,
				uLoop, 1f);
	}
}
