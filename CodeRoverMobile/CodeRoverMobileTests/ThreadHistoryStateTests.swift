import XCTest
@testable import CodeRoverMobile

@MainActor
final class ThreadHistoryStateTests: XCTestCase {
    func testTailMergeCreatesGapWhenLatestWindowSkipsMiddleHistory() {
        let service = makeService()
        let threadID = "thread-gap"

        service.messagesByThread[threadID] = makeMessages(threadID: threadID, range: 1...20)

        service.mergeHistoryWindow(
            threadId: threadID,
            mode: .tail,
            historyMessages: makeMessages(threadID: threadID, range: 71...120),
            oldestAnchor: makeAnchor(index: 71),
            newestAnchor: makeAnchor(index: 120),
            hasOlder: true,
            hasNewer: false
        )

        let state = try XCTUnwrap(service.historyStateByThread[threadID])
        XCTAssertEqual(state.segments.count, 2)
        XCTAssertEqual(state.gaps.count, 1)
        XCTAssertEqual(state.segments.first?.oldestAnchor, makeAnchor(index: 1))
        XCTAssertEqual(state.segments.last?.newestAnchor, makeAnchor(index: 120))
        XCTAssertTrue(state.hasOlderOnServer)
        XCTAssertFalse(state.hasNewerOnServer)
    }

    func testBeforeMergeClosesGapOnceMissingRangeArrives() {
        let service = makeService()
        let threadID = "thread-close-gap"

        service.messagesByThread[threadID] = makeMessages(threadID: threadID, range: 1...20)
        service.mergeHistoryWindow(
            threadId: threadID,
            mode: .tail,
            historyMessages: makeMessages(threadID: threadID, range: 71...120),
            oldestAnchor: makeAnchor(index: 71),
            newestAnchor: makeAnchor(index: 120),
            hasOlder: true,
            hasNewer: false
        )

        service.mergeHistoryWindow(
            threadId: threadID,
            mode: .before,
            historyMessages: makeMessages(threadID: threadID, range: 21...70),
            oldestAnchor: makeAnchor(index: 21),
            newestAnchor: makeAnchor(index: 70),
            hasOlder: false,
            hasNewer: false
        )

        let state = try XCTUnwrap(service.historyStateByThread[threadID])
        XCTAssertEqual(state.segments.count, 1)
        XCTAssertTrue(state.gaps.isEmpty)
        XCTAssertEqual(state.oldestLoadedAnchor, makeAnchor(index: 1))
        XCTAssertEqual(state.newestLoadedAnchor, makeAnchor(index: 120))
        XCTAssertFalse(state.hasOlderOnServer)
    }

    func testEnsureThreadResumedStillAllowsTailHistoryRefresh() async throws {
        let service = makeService()
        let threadID = "thread-tail-refresh"
        service.isConnected = true
        service.isInitialized = true

        var recordedMethods: [String] = []
        service.requestTransportOverride = { method, _ in
            recordedMethods.append(method)
            switch method {
            case "thread/resume":
                return RPCMessage(
                    id: .string(UUID().uuidString),
                    result: .object([
                        "thread": self.makeThreadPayload(
                            threadID: threadID,
                            title: "Resume",
                            messageRange: 1...2
                        ),
                    ]),
                    includeJSONRPC: false
                )
            case "thread/read":
                return RPCMessage(
                    id: .string(UUID().uuidString),
                    result: .object([
                        "thread": self.makeThreadPayload(
                            threadID: threadID,
                            title: "Tail",
                            messageRange: 71...120
                        ),
                        "historyWindow": .object([
                            "oldestAnchor": self.makeAnchorObject(index: 71),
                            "newestAnchor": self.makeAnchorObject(index: 120),
                            "hasOlder": .bool(true),
                            "hasNewer": .bool(false),
                        ]),
                    ]),
                    includeJSONRPC: false
                )
            default:
                XCTFail("Unexpected method \(method)")
                return RPCMessage(id: .string(UUID().uuidString), result: .object([:]), includeJSONRPC: false)
            }
        }

        _ = try await service.ensureThreadResumed(threadId: threadID)
        XCTAssertFalse(service.hydratedThreadIDs.contains(threadID))

        try await service.loadThreadHistoryIfNeeded(threadId: threadID)

        XCTAssertEqual(recordedMethods, ["thread/resume", "thread/read"])
        XCTAssertEqual(service.messagesByThread[threadID]?.first?.itemId, "item-1")
        XCTAssertEqual(service.messagesByThread[threadID]?.last?.itemId, "item-120")
        XCTAssertEqual(service.historyStateByThread[threadID]?.segments.count, 2)
    }

    func testCachedResumeAndHydrationDoNotBlockReloadWhenLocalMessagesAreEmpty() async throws {
        let service = makeService()
        let threadID = "thread-reload-empty"
        service.isConnected = true
        service.isInitialized = true
        service.threads = [ConversationThread(id: threadID, title: "Thread")]
        service.resumedThreadIDs.insert(threadID)
        service.hydratedThreadIDs.insert(threadID)
        service.messagesByThread[threadID] = []

        var recordedMethods: [String] = []
        service.requestTransportOverride = { method, _ in
            recordedMethods.append(method)
            switch method {
            case "thread/resume":
                return RPCMessage(
                    id: .string(UUID().uuidString),
                    result: .object([
                        "thread": self.makeThreadPayload(
                            threadID: threadID,
                            title: "Resume",
                            messageRange: 1...2
                        ),
                    ]),
                    includeJSONRPC: false
                )
            case "thread/read":
                return RPCMessage(
                    id: .string(UUID().uuidString),
                    result: .object([
                        "thread": self.makeThreadPayload(
                            threadID: threadID,
                            title: "Tail",
                            messageRange: 11...20
                        ),
                        "historyWindow": .object([
                            "oldestAnchor": self.makeAnchorObject(index: 11),
                            "newestAnchor": self.makeAnchorObject(index: 20),
                            "hasOlder": .bool(true),
                            "hasNewer": .bool(false),
                        ]),
                    ]),
                    includeJSONRPC: false
                )
            default:
                XCTFail("Unexpected method \(method)")
                return RPCMessage(id: .string(UUID().uuidString), result: .object([:]), includeJSONRPC: false)
            }
        }

        _ = try await service.ensureThreadResumed(threadId: threadID)
        try await service.loadThreadHistoryIfNeeded(threadId: threadID)

        XCTAssertEqual(recordedMethods, ["thread/resume", "thread/read"])
        XCTAssertEqual(service.messagesByThread[threadID]?.first?.itemId, "item-1")
        XCTAssertEqual(service.messagesByThread[threadID]?.last?.itemId, "item-20")
    }

    func testForceTailRefreshMergesRunningThreadSnapshot() async throws {
        let service = makeService()
        let threadID = "thread-running-refresh"
        service.isConnected = true
        service.isInitialized = true
        service.runningThreadIDs.insert(threadID)
        service.messagesByThread[threadID] = [
            ChatMessage(
                id: "assistant-local",
                threadId: threadID,
                role: .assistant,
                text: "partial",
                createdAt: Date(timeIntervalSince1970: 100),
                turnId: "turn-1",
                itemId: "item-100",
                isStreaming: true,
                orderIndex: 1
            ),
        ]

        service.requestTransportOverride = { method, _ in
            XCTAssertEqual(method, "thread/read")
            return RPCMessage(
                id: .string(UUID().uuidString),
                result: .object([
                    "thread": self.makeThreadPayload(
                        threadID: threadID,
                        title: "Tail",
                        messageRange: 100...101
                    ),
                    "historyWindow": .object([
                        "oldestAnchor": self.makeAnchorObject(index: 100),
                        "newestAnchor": self.makeAnchorObject(index: 101),
                        "hasOlder": .bool(true),
                        "hasNewer": .bool(false),
                    ]),
                ]),
                includeJSONRPC: false
            )
        }

        try await service.loadThreadHistoryIfNeeded(threadId: threadID, forceRefresh: true)

        let messages = try XCTUnwrap(service.messagesByThread[threadID])
        XCTAssertEqual(messages.count, 2)
        XCTAssertEqual(messages.first?.itemId, "item-100")
        XCTAssertTrue(messages.first?.text.contains("message-100") == true)
        XCTAssertTrue(messages.first?.isStreaming == true)
        XCTAssertEqual(messages.last?.itemId, "item-101")
    }

    func testSyncActiveThreadStateRefreshesHistoryEvenWhenThreadIsStillRunning() async throws {
        let service = makeService()
        let threadID = "thread-active-running"
        service.isConnected = true
        service.isInitialized = true
        service.runningThreadIDs.insert(threadID)
        service.activeTurnIdByThread[threadID] = "turn-1"

        var recordedRequestKinds: [String] = []
        service.requestTransportOverride = { method, params in
            XCTAssertEqual(method, "thread/read")
            let paramsObject = params?.objectValue ?? [:]
            if paramsObject["includeTurns"]?.boolValue == true {
                recordedRequestKinds.append("turn-state")
                return RPCMessage(
                    id: .string(UUID().uuidString),
                    result: .object([
                        "thread": .object([
                            "id": .string(threadID),
                            "provider": .string("codex"),
                            "turns": .array([
                                .object([
                                    "id": .string("turn-1"),
                                    "status": .string("running"),
                                    "items": .array([]),
                                ]),
                            ]),
                        ]),
                    ]),
                    includeJSONRPC: false
                )
            }

            recordedRequestKinds.append("history-tail")
            return RPCMessage(
                id: .string(UUID().uuidString),
                result: .object([
                    "thread": self.makeThreadPayload(
                        threadID: threadID,
                        title: "Tail",
                        messageRange: 201...202
                    ),
                    "historyWindow": .object([
                        "oldestAnchor": self.makeAnchorObject(index: 201),
                        "newestAnchor": self.makeAnchorObject(index: 202),
                        "hasOlder": .bool(true),
                        "hasNewer": .bool(false),
                    ]),
                ]),
                includeJSONRPC: false
            )
        }

        await service.syncActiveThreadState(threadId: threadID)

        XCTAssertEqual(recordedRequestKinds, ["turn-state", "history-tail"])
        XCTAssertEqual(service.messagesByThread[threadID]?.last?.itemId, "item-202")
    }

    func testIncomingDeltaSchedulesAfterCatchUpWhenTailHasGap() async throws {
        let service = makeService()
        let threadID = "thread-event-after-gap"
        service.isConnected = true
        service.isInitialized = true
        service.activeThreadId = threadID
        service.threads = [ConversationThread(id: threadID, title: "Thread", provider: "codex")]
        service.runningThreadIDs.insert(threadID)
        service.activeTurnIdByThread[threadID] = "turn-0"
        service.messagesByThread[threadID] = makeMessages(threadID: threadID, range: 1...1)

        let afterExpectation = expectation(description: "after history request")
        service.requestTransportOverride = { method, params in
            XCTAssertEqual(method, "thread/read")
            let historyObject = try XCTUnwrap(params?.objectValue?["history"]?.objectValue)
            XCTAssertEqual(historyObject["mode"]?.stringValue, "after")
            XCTAssertEqual(historyObject["anchor"]?.objectValue?["itemId"]?.stringValue, "item-1")
            afterExpectation.fulfill()
            return RPCMessage(
                id: .string(UUID().uuidString),
                result: .object([
                    "thread": self.makeThreadPayload(
                        threadID: threadID,
                        title: "After",
                        messageRange: 2...3
                    ),
                    "historyWindow": .object([
                        "oldestAnchor": self.makeAnchorObject(index: 2),
                        "newestAnchor": self.makeAnchorObject(index: 3),
                        "hasOlder": .bool(false),
                        "hasNewer": .bool(false),
                    ]),
                ]),
                includeJSONRPC: false
            )
        }

        service.handleNotification(
            method: "item/agentMessage/delta",
            params: .object([
                "threadId": .string(threadID),
                "turnId": .string("turn-0"),
                "itemId": .string("item-3"),
                "delta": .string("partial-3"),
            ])
        )

        await fulfillment(of: [afterExpectation], timeout: 1.0)
        XCTAssertEqual(service.messagesByThread[threadID]?.map(\.itemId), ["item-1", "item-2", "item-3"])
    }

    func testIncomingDeltaSkipsAfterCatchUpWhenPreviousItemMatchesTail() async throws {
        let service = makeService()
        let threadID = "thread-event-direct-tail"
        service.isConnected = true
        service.isInitialized = true
        service.activeThreadId = threadID
        service.threads = [ConversationThread(id: threadID, title: "Thread", provider: "codex")]
        service.runningThreadIDs.insert(threadID)
        service.activeTurnIdByThread[threadID] = "turn-1"
        service.messagesByThread[threadID] = makeMessages(threadID: threadID, range: 1...10)

        service.requestTransportOverride = { method, _ in
            XCTFail("Did not expect realtime catch-up request for \(method)")
            return RPCMessage(id: .string(UUID().uuidString), result: .object([:]), includeJSONRPC: false)
        }

        service.handleNotification(
            method: "item/agentMessage/delta",
            params: .object([
                "threadId": .string(threadID),
                "turnId": .string("turn-1"),
                "itemId": .string("item-11"),
                "previousItemId": .string("item-10"),
                "delta": .string("partial-11"),
            ])
        )

        try await Task.sleep(nanoseconds: 250_000_000)
        XCTAssertEqual(service.messagesByThread[threadID]?.last?.itemId, "item-11")
    }

    private func makeService() -> CodeRoverService {
        let suiteName = "ThreadHistoryStateTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName) ?? .standard
        defaults.removePersistentDomain(forName: suiteName)
        return CodeRoverService(defaults: defaults)
    }

    private func makeMessages(threadID: String, range: ClosedRange<Int>) -> [ChatMessage] {
        range.map { index in
            ChatMessage(
                id: "message-\(index)",
                threadId: threadID,
                role: .assistant,
                text: "message-\(index)",
                createdAt: Date(timeIntervalSince1970: TimeInterval(index)),
                turnId: "turn-\(index / 10)",
                itemId: "item-\(index)",
                orderIndex: index
            )
        }
    }

    private func makeAnchor(index: Int) -> ThreadHistoryAnchor {
        ThreadHistoryAnchor(
            itemId: "item-\(index)",
            createdAt: Date(timeIntervalSince1970: TimeInterval(index)),
            turnId: "turn-\(index / 10)"
        )
    }

    private func makeAnchorObject(index: Int) -> JSONValue {
        .object([
            "itemId": .string("item-\(index)"),
            "createdAt": .string(ISO8601DateFormatter().string(from: Date(timeIntervalSince1970: TimeInterval(index)))),
            "turnId": .string("turn-\(index / 10)"),
        ])
    }

    private func makeThreadPayload(
        threadID: String,
        title: String,
        messageRange: ClosedRange<Int>
    ) -> JSONValue {
        .object([
            "id": .string(threadID),
            "title": .string(title),
            "provider": .string("codex"),
            "turns": .array([
                .object([
                    "id": .string("turn-\(messageRange.lowerBound / 10)"),
                    "items": .array(
                        messageRange.map { index in
                            .object([
                                "id": .string("item-\(index)"),
                                "type": .string("assistantMessage"),
                                "createdAt": .string(ISO8601DateFormatter().string(from: Date(timeIntervalSince1970: TimeInterval(index)))),
                                "content": .array([
                                    .object([
                                        "type": .string("text"),
                                        "text": .string("message-\(index)"),
                                    ]),
                                ]),
                            ])
                        }
                    ),
                ]),
            ]),
        ])
    }
}
