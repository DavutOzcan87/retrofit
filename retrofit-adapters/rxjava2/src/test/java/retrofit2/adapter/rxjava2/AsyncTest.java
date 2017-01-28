/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.adapter.rxjava2;

import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;

public final class AsyncTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Completable completable();
  }

  private Service service;
  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
        .build();
    service = retrofit.create(Service.class);

    server.setDispatcher(new QueueDispatcher() {
      @Override public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        awaitResponseTrigger();
        return super.dispatch(request);
      }
    });
  }

  @Test public void async() throws InterruptedException {
    server.enqueue(new MockResponse());

    TestObserver<Void> observer = new TestObserver<>();
    service.completable().subscribe(observer);
    assertFalse(observer.await(1, SECONDS));

    triggerResponse();
    observer.awaitDone(1, SECONDS);
  }

  private synchronized void awaitResponseTrigger() throws InterruptedException {
    wait();
  }

  private synchronized void triggerResponse() {
    notifyAll();
  }
}
