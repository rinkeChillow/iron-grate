package com.rinke_.irongrate;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IronGrate implements ModInitializer {
	public static final String MOD_ID = "irongrate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {


		LOGGER.info("hehe boi");
	}
}