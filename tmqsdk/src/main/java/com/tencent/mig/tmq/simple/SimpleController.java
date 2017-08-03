/*
 * Copyright (C) 2015 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.tencent.mig.tmq.simple;

import com.tencent.mig.tmq.abs.AbstractController;

public class SimpleController extends AbstractController<SimpleTmqMsg> {

	public SimpleController()
	{
		super();
	}

	@Override
	public boolean report(String tag, Object msg) {
		synchronized (this) {
//			if (countDownLatch == null || countDownLatch.getCount() == 0) {
			// 为了兼容排他消息，在countDownLatch.getCount() == 0后继续收消息
//			if (countDownLatch == null)
			// 上次的修改，会造成reset后，如有report上报,会造成提前工作，直接空指针异常，所以这里引入state状态
			if (! state) {
				return true;
			}

			SimpleTmqMsg tmqMsg = msg instanceof SimpleTmqMsg ? (SimpleTmqMsg) msg : new SimpleTmqMsg(tag, msg.toString());

			if (logger.append(tmqMsg) && mode.match(tmqMsg)) {
				logger.appendCheckedMsg(tmqMsg);

				if (mode.keyMatched(tmqMsg))
				{
					countDownLatch.countDown();
				}
				return true;
			}
			return true;
		}
	}

	@Override
	public void willCare(SimpleTmqMsg msg) {
		state = true;
		if (null == msg || msg.equals(SimpleTmqMsg.NULL))
		{
			// 加入NULL消息后，按照严格匹配模式自然任何消息都不会和这条消息匹配上，就会判定不通过
			mode.willCare(SimpleTmqMsg.NULL);
			return;
		}
		mode.willCare(msg);
	}
}
